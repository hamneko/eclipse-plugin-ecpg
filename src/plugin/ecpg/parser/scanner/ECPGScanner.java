package plugin.ecpg.parser.scanner;

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

	private boolean isSqlcaProcessed = false;

	public ECPGScanner(FileContent fileContent, IScannerInfo info, ParserLanguage language, IParserLogService log,
			IScannerExtensionConfiguration configuration, IncludeFileContentProvider readerFactory) {
		super(fileContent, info, language, log, configuration, readerFactory);
	}

	@Override
	public IToken nextToken() throws EndOfFileException {
		if (tokens().peek() != null) {
			return clearPrefetchedToken(tokens().poll());
		}
		IToken nextToken = super.nextToken();
		// Process "EXEC" to ";" tokens().
		if ("exec".equalsIgnoreCase(nextToken.toString())) {
			if (!isSqlcaProcessed) {
				OverrideToken dummyToken = new OverrideToken();
				dummyToken.setOffset(-1);
				dummyToken.setEndOffset(0);
				PluginTokenUtil.addSqlcaTokensEcpg(this, dummyToken);
				// Initialize sqlca struct.
				tokens().add(PluginTokenUtil.createStructToken(this, dummyToken));
				tokens().add(PluginTokenUtil.createIdentifierToken(this, dummyToken, "sqlca"));
				tokens().add(PluginTokenUtil.createIdentifierToken(this, dummyToken, "sqlca"));
				tokens().add(PluginTokenUtil.createSemiToken(this, dummyToken));
				isSqlcaProcessed = true;
			}
			while (true) {
				nextToken = super.nextToken();
				if ("sqlca".equalsIgnoreCase(nextToken.toString())) {
					PluginTokenUtil.addSqlcaTokensEcpg(this, nextToken);
					// Initialize sqlca struct.
					tokens().add(PluginTokenUtil.createStructToken(this, nextToken));
					tokens().add(PluginTokenUtil.createIdentifierToken(this, nextToken, "sqlca"));
					tokens().add(PluginTokenUtil.createIdentifierToken(this, nextToken, "sqlca"));
					tokens().add(PluginTokenUtil.createSemiToken(this, nextToken));
					continue;
				}
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
				if (":".equals(nextToken.toString())) {
					nextToken = super.nextToken();
					// Modify tokens to be treated as variables.
					tokens().add(nextToken);
					tokens().add(PluginTokenUtil.createAssignToken(this, nextToken));
					tokens().add(PluginTokenUtil.createIntegerToken(this, nextToken, "0"));
					tokens().add(PluginTokenUtil.createSemiToken(this, nextToken));
					continue;
				}
				if (";".equals(nextToken.toString())) {
					tokens().add(nextToken);
					return clearPrefetchedToken(tokens().poll());
				}
			}
		}
		return nextToken;
	}

}
