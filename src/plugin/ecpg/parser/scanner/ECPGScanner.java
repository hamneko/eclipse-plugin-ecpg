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
				isSqlcaProcessed = true;
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

		// Process ECPG specific sqlca
//		if ("sqlca".equalsIgnoreCase(nextToken.toString())) {
//			//
//			nextToken = super.nextToken();
//			if (!".".equals(nextToken.toString())) {
//				return nextToken;
//			}
//			nextToken = super.nextToken();
//			if ("sqlcaid".equalsIgnoreCase(nextToken.toString())) {
//				// char sqlcaid[8];
//				nextToken.setType(IToken.tINTEGER);
//			} else if ("sqlabc".equalsIgnoreCase(nextToken.toString())) {
//				// long sqlabc;
//				nextToken.setType(IToken.tINTEGER);
//			} else if ("sqlcode".equalsIgnoreCase(nextToken.toString())) {
//				// long sqlcode;
//				nextToken.setType(IToken.tINTEGER);
//			} else if ("sqlerrm".equalsIgnoreCase(nextToken.toString())) {
//				// sqlerrm
//				nextToken = super.nextToken();
//				if (".".equals(nextToken.toString())) {
//					nextToken = super.nextToken();
//					if ("sqlerrml".equalsIgnoreCase(nextToken.toString())
//							|| "sqlerrmc".equalsIgnoreCase(nextToken.toString())) {
//						// int sqlerrml; or char sqlerrmc[SQLERRMC_LEN];
//						nextToken.setType(IToken.tINTEGER);
//					}
//				}
//			} else if ("sqlerrp".equalsIgnoreCase(nextToken.toString())) {
//				// char sqlerrp[8];
//				nextToken.setType(IToken.tINTEGER);
//			} else if ("sqlerrd".equalsIgnoreCase(nextToken.toString())) {
//				// long sqlerrd[6];
//				nextToken.setType(IToken.tINTEGER);
//			} else if ("sqlwarn".equalsIgnoreCase(nextToken.toString())) {
//				// char sqlwarn[8];
//				nextToken.setType(IToken.tINTEGER);
//			} else if ("sqlstate".equalsIgnoreCase(nextToken.toString())) {
//				// char sqlstate[5];
//				nextToken.setType(IToken.tINTEGER);
//			}
//		}
		return nextToken;
	}

}
