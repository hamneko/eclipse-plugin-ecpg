package plugin.ecpg.parser.scanner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.parser.IScannerExtensionConfiguration;
import org.eclipse.cdt.core.parser.EndOfFileException;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IToken;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ParserLanguage;

import plugin.common.parser.scanner.OverrideToken;
import plugin.common.parser.scanner.PluginScanner;
import plugin.common.parser.scanner.PluginTokenUtil;

@SuppressWarnings("restriction")
public class ECPGScanner extends PluginScanner {

	private boolean sqlcaProcessed = false;

	private List<ExecSqlPosition> execSqlPositions;

	private Map<String, String> cursorVariables = new HashMap<>();

	private boolean colonStarted = false;

	private boolean immediateAfterComma = false;

	public ECPGScanner(FileContent fileContent, IScannerInfo info, ParserLanguage language, IParserLogService log,
			IScannerExtensionConfiguration configuration, IncludeFileContentProvider readerFactory) {
		super(fileContent, info, language, log, configuration, readerFactory);
	}

	public ECPGScanner(FileContent fileContent, IScannerInfo info, ParserLanguage language, IParserLogService log,
			IScannerExtensionConfiguration configuration, IncludeFileContentProvider readerFactory,
			List<ExecSqlPosition> execSqlPositions) {
		super(fileContent, info, language, log, configuration, readerFactory);
		this.execSqlPositions = execSqlPositions;
	}

	@Override
	public IToken nextToken() throws EndOfFileException {
		if (tokens().peek() != null) {
			return clearPrefetchedToken(tokens().poll());
		}
		int from = 0, to = 0;
		IToken nextToken = super.nextToken();
		// Process "EXEC" to ";" tokens().
		if ("exec".equalsIgnoreCase(nextToken.toString())) {
			from = nextToken.getOffset();
			if (!sqlcaProcessed) {
				OverrideToken dummyToken = new OverrideToken();
				dummyToken.setOffset(nextToken.getOffset());
				dummyToken.setEndOffset(nextToken.getOffset() - 1);
				PluginTokenUtil.addSqlcaTokensEcpg(this, dummyToken);
				// Initialize sqlca struct.
				tokens().add(PluginTokenUtil.createStructToken(this, dummyToken));
				tokens().add(PluginTokenUtil.createIdentifierToken(this, dummyToken, "sqlca"));
				tokens().add(PluginTokenUtil.createIdentifierToken(this, dummyToken, "sqlca"));
				tokens().add(PluginTokenUtil.createSemiToken(this, dummyToken));
				// SQLSTATE
				tokens().add(PluginTokenUtil.createCharToken(this, dummyToken));
				tokens().add(PluginTokenUtil.createIdentifierToken(this, dummyToken, "SQLSTATE"));
				tokens().add(PluginTokenUtil.createSemiToken(this, dummyToken));
				// SQLCODE
				tokens().add(PluginTokenUtil.createLongToken(this, dummyToken));
				tokens().add(PluginTokenUtil.createIdentifierToken(this, dummyToken, "SQLCODE"));
				tokens().add(PluginTokenUtil.createSemiToken(this, dummyToken));
				sqlcaProcessed = true;
			}
			while (true) {
				nextToken = super.nextToken();
				if ("call".equalsIgnoreCase(nextToken.toString())) {
					return super.nextToken();
				}
				if ("do".equalsIgnoreCase(nextToken.toString())) {
					nextToken = super.nextToken();
					if ("break".equalsIgnoreCase(nextToken.toString())
							|| "continue".equalsIgnoreCase(nextToken.toString())) {
						// ignore those tokens().
						continue;
					}
					return nextToken;
				}
				if ("declare".equalsIgnoreCase(nextToken.toString())) {
					nextToken = super.nextToken();
					if ("section".equalsIgnoreCase(nextToken.toString())) {
						continue;
					}
					// Modify tokens to be treated as variable declaration.
					cursorVariables.put(nextToken.toString(), "");
					tokens().add(PluginTokenUtil.createCharToken(null, nextToken));
					tokens().add(nextToken);
					tokens().add(PluginTokenUtil.createSemiToken(this, nextToken));
					continue;
				}
				if (cursorVariables.containsKey(nextToken.toString())) {
					// Modify tokens to be treated as variables.
					tokens().add(nextToken);
					tokens().add(PluginTokenUtil.createAssignToken(this, nextToken));
					tokens().add(PluginTokenUtil.createLiteralToken(this, nextToken, ""));
					tokens().add(PluginTokenUtil.createSemiToken(this, nextToken));
					continue;
				}
				if (":".equals(nextToken.toString())) {
					nextToken = super.nextToken();
					//
					to = nextToken.getOffset();
					// Modify tokens to be treated as variables.
					tokens().add(nextToken);
					tokens().add(PluginTokenUtil.createAssignToken(this, nextToken));
					tokens().add(PluginTokenUtil.createIntegerToken(this, nextToken, "0"));
					tokens().add(PluginTokenUtil.createSemiToken(this, nextToken));
					colonStarted = true;
					immediateAfterComma = false;
					continue;
				}
				if (colonStarted && !";".equals(nextToken.toString())) {
					// Ignore comma
					if (",".equals(nextToken.toString())) {
						immediateAfterComma = true;
						continue;
					}
					if (!immediateAfterComma) {
						continue;
					}
					immediateAfterComma = false;
					to = nextToken.getOffset();
					// Modify tokens to be treated as variables.
					tokens().add(nextToken);
					tokens().add(PluginTokenUtil.createAssignToken(this, nextToken));
					tokens().add(PluginTokenUtil.createIntegerToken(this, nextToken, "0"));
					tokens().add(PluginTokenUtil.createSemiToken(this, nextToken));
					continue;
				}
				if (";".equals(nextToken.toString())) {
					colonStarted = false;
					immediateAfterComma = false;
					if (to == 0) {
						to = nextToken.getOffset();
					}
					execSqlPositions.add(new ExecSqlPosition(from, to));
					tokens().add(nextToken);
					return clearPrefetchedToken(tokens().poll());
				}
			}
		}
		return nextToken;
	}

	public class ExecSqlPosition {
		int from;
		int to;

		public ExecSqlPosition(int from, int to) {
			this.from = from;
			this.to = to;
		}

		public int getFrom() {
			return from;
		}

		public int getTo() {
			return to;
		}

		@Override
		public String toString() {
			return from + ":" + to;
		}

	}
}
