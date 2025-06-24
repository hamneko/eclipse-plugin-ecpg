package plugin.ecpg.parser.scanner;

import java.util.ArrayDeque;
import java.util.Queue;

import org.eclipse.cdt.core.dom.parser.IScannerExtensionConfiguration;
import org.eclipse.cdt.core.parser.EndOfFileException;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IToken;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ParserLanguage;
import org.eclipse.cdt.internal.core.parser.scanner.CPreprocessor;
import org.eclipse.cdt.internal.core.parser.scanner.TokenUtil;
import org.eclipse.cdt.internal.core.parser.scanner.TokenWithImage;

@SuppressWarnings("restriction")
public class ECPGScanner extends CPreprocessor {

	/*
	 * Inner Queue for token manipulation.
	 */
	private Queue<IToken> tokens = new ArrayDeque<>();

	public ECPGScanner(FileContent fileContent, IScannerInfo info, ParserLanguage language, IParserLogService log,
			IScannerExtensionConfiguration configuration, IncludeFileContentProvider readerFactory) {
		super(fileContent, info, language, log, configuration, readerFactory);
	}

	@Override
	public IToken nextToken() throws EndOfFileException {
		if (tokens.peek() != null) {
			return tokens.poll();
		}
		IToken nextToken = super.nextToken();

		// Ignore from "EXEC" to ";" tokens.
		if ("exec".equalsIgnoreCase(nextToken.toString())) {
			IToken peekNextToken;
			while (true) {
				nextToken = super.nextToken();
				if ("call".equalsIgnoreCase(nextToken.toString())) {
					return super.nextToken();
				}
				if ("do".equalsIgnoreCase(nextToken.toString())) {
					peekNextToken = super.nextToken();
					nextToken.setNext(null);
					if ("break".equalsIgnoreCase(peekNextToken.toString())
							|| "continue".equalsIgnoreCase(peekNextToken.toString())) {
						// ignore those tokens
						continue;
					}
					return peekNextToken;
				}
				if (":".equalsIgnoreCase(nextToken.toString())) {
					nextToken = super.nextToken();
					// To prevent warning, assign dummy value.
					IToken t1 = new TokenWithImage(IToken.tASSIGN, this, nextToken.getOffset(),
							nextToken.getEndOffset(), TokenUtil.getImage(IToken.tASSIGN));
					IToken t2 = new TokenWithImage(IToken.tSTRING, this, nextToken.getOffset(),
							nextToken.getEndOffset(), "\"FAKE_STRING\"".toCharArray());
					peekNextToken = super.nextToken();
					nextToken.setNext(null);
					if (!";".equals(peekNextToken.toString())) {
						IToken t3 = new TokenWithImage(IToken.tCOMMA, this, nextToken.getOffset(),
								nextToken.getEndOffset(), TokenUtil.getImage(IToken.tCOMMA));
						tokens.add(nextToken);
						tokens.add(t1);
						tokens.add(t2);
						tokens.add(t3);
						continue;
					} else {
						tokens.add(nextToken);
						tokens.add(t1);
						tokens.add(t2);
						tokens.add(peekNextToken);
						return tokens.poll();
					}
				}
				if (";".equals(nextToken.toString())) {
					peekNextToken = super.nextToken();
					nextToken.setNext(null);
					if (!"exec".equalsIgnoreCase(peekNextToken.toString())) {
						tokens.add(peekNextToken);
						return nextToken;
					}
				}
			}
		}

		// sqlca
		if ("sqlca".equalsIgnoreCase(nextToken.toString())) {
			//
			nextToken = super.nextToken();
			if (!".".equals(nextToken.toString())) {
				return nextToken;
			}
			//
			nextToken = super.nextToken();
			if ("sqlcaid".equalsIgnoreCase(nextToken.toString())) {
				// char sqlcaid[8];
				nextToken.setType(IToken.tINTEGER);
			} else if ("sqlabc".equalsIgnoreCase(nextToken.toString())) {
				// long sqlabc;
				nextToken.setType(IToken.tINTEGER);
			} else if ("sqlcode".equalsIgnoreCase(nextToken.toString())) {
				// long sqlcode;
				nextToken.setType(IToken.tINTEGER);
			} else if ("sqlerrm".equalsIgnoreCase(nextToken.toString())) {
				// sqlerrm
				nextToken = super.nextToken();
				if (".".equals(nextToken.toString())) {
					nextToken = super.nextToken();
					if ("sqlerrml".equalsIgnoreCase(nextToken.toString())
							|| "sqlerrmc".equalsIgnoreCase(nextToken.toString())) {
						// int sqlerrml; or char sqlerrmc[SQLERRMC_LEN];
						nextToken.setType(IToken.tINTEGER);
					}
				}
			} else if ("sqlerrp".equalsIgnoreCase(nextToken.toString())) {
				// char sqlerrp[8];
				nextToken.setType(IToken.tINTEGER);
			} else if ("sqlerrd".equalsIgnoreCase(nextToken.toString())) {
				// long sqlerrd[6];
				nextToken.setType(IToken.tINTEGER);
			} else if ("sqlwarn".equalsIgnoreCase(nextToken.toString())) {
				// char sqlwarn[8];
				nextToken.setType(IToken.tINTEGER);
			} else if ("sqlstate".equalsIgnoreCase(nextToken.toString())) {
				// char sqlstate[5];
				nextToken.setType(IToken.tINTEGER);
			}
		}
		return nextToken;
	}

}
