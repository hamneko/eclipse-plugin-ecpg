package plugin.ecpg;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.parser.c.ICParserExtensionConfiguration;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.EndOfFileException;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScanner;
import org.eclipse.cdt.core.parser.ParserMode;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;
import org.eclipse.cdt.internal.core.dom.parser.BacktrackException;
import org.eclipse.cdt.internal.core.dom.parser.c.GNUCSourceParser;

import plugin.ecpg.parser.scanner.ECPGScanner.ExecSqlPosition;

@SuppressWarnings("restriction")
public class ECPGSourceParser extends GNUCSourceParser {

	private List<ExecSqlPosition> execSqlPositions;

	public ECPGSourceParser(IScanner scanner, ParserMode parserMode, IParserLogService logService,
			ICParserExtensionConfiguration config, IIndex index) {
		this(scanner, parserMode, logService, config, index, null);
	}

	public ECPGSourceParser(IScanner scanner, ParserMode parserMode, IParserLogService logService,
			ICParserExtensionConfiguration config, IIndex index, List<ExecSqlPosition> execSqlPositions) {
		super(scanner, parserMode, logService, config, index);
		this.execSqlPositions = execSqlPositions;
	}

	@Override
	protected IASTStatement statement() throws EndOfFileException, BacktrackException {
		IASTStatement statement = super.statement();
		if (execSqlPositions == null) {
			return statement;
		}
		ASTNode node = (ASTNode) statement;
		if (inPosition(node.getOffset())) {
			return statement();
		}
		return statement;
	}

	private boolean inPosition(int pos) {
		for (ExecSqlPosition execSqlPosition : execSqlPositions) {
			if (execSqlPosition.getFrom() <= pos && (execSqlPosition.getTo() - 2) >= pos) {
				return true;
			}
		}
		return false;
	}
}
