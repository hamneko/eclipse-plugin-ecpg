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

import plugin.common.parser.scanner.ExecSqlPosition;
import plugin.common.parser.scanner.OverrideToken;
import plugin.common.parser.scanner.PluginScanner;
import plugin.common.parser.scanner.PluginTokenUtil;

@SuppressWarnings("restriction")
public class ECPGScanner extends PluginScanner {

	private boolean sqlcaProcessed = false;

	private List<ExecSqlPosition> execSqlPositions;

	private Map<String, String> cursorVariables = new HashMap<>();

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
		// Initialize required variables
		int from = 0, to = 0;
		boolean colonStarted = false;
		boolean immediateAfterComma = false;
		boolean immediateAfterVariable = false;
		IToken nextToken = super.nextToken();
		// Process "EXEC" to ";" tokens().
		if ("exec".equalsIgnoreCase(nextToken.toString())) {
			from = nextToken.getOffset();
			if (!sqlcaProcessed) {
				declareSqlca(nextToken);
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
				if ("declare".equalsIgnoreCase(nextToken.toString())
						|| "prepare".equalsIgnoreCase(nextToken.toString())) {
					nextToken = super.nextToken();
					if ("section".equalsIgnoreCase(nextToken.toString())) {
						continue;
					}
					// Modify tokens to be treated as variable declaration.
					cursorVariables.put(nextToken.toString(), "");
					tokens().add(PluginTokenUtil.createCharToken(this, nextToken));
					tokens().add(nextToken);
					tokens().add(PluginTokenUtil.createSemiToken(this, nextToken));
					continue;
				}
				if (cursorVariables.containsKey(nextToken.toString())) {
					to = nextToken.getOffset();
					immediateAfterVariable = true;
					// Modify tokens to be treated as variables.
					tokens().add(nextToken);
					PluginTokenUtil.addAssignLiteralTokens(this, nextToken);
					continue;
				}
				if (":".equals(nextToken.toString())) {
					nextToken = super.nextToken();
					to = nextToken.getOffset();
					immediateAfterVariable = true;
					// Modify tokens to be treated as variables.
					tokens().add(nextToken);
					PluginTokenUtil.addAssignIntegerTokens(this, nextToken);
					colonStarted = true;
					immediateAfterComma = false;
					continue;
				}
				if (colonStarted && !";".equals(nextToken.toString())) {
					immediateAfterVariable = false;
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
					immediateAfterVariable = true;
					// Modify tokens to be treated as variables.
					tokens().add(nextToken);
					PluginTokenUtil.addAssignIntegerTokens(this, nextToken);
					continue;
				}
				if (";".equals(nextToken.toString())) {
					colonStarted = false;
					immediateAfterComma = false;
					if (!immediateAfterVariable) {
						to = nextToken.getOffset() + 1;
					}
					execSqlPositions.add(new ExecSqlPosition(from, to));
					tokens().add(nextToken);
					return clearPrefetchedToken(tokens().poll());
				}
				immediateAfterVariable = false;
			}
		}
		return nextToken;
	}

	private void declareSqlca(IToken nextToken) {
		OverrideToken dummyToken = new OverrideToken();
		dummyToken.setOffset(nextToken.getOffset());
		dummyToken.setEndOffset(nextToken.getOffset() - 1);
		PluginTokenUtil.addSqlcaTokensEcpg(this, dummyToken);
		// Initialize sqlca struct.
		PluginTokenUtil.addSqlcaStructTokens(this, dummyToken);
		// SQLSTATE
		PluginTokenUtil.addSqlStateTokens(this, dummyToken);
		// SQLCODE
		PluginTokenUtil.addSqlCodeTokens(this, dummyToken);
	}

}
