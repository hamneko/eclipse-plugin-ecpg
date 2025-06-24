package plugin.ecpg;

import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScanner;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;

import plugin.ecpg.parser.scanner.ECPGScanner;

public class ECPGLanguage extends GCCLanguage {

	@Override
	public String getId() {
		return "plugin.ecpg.editor";
	}

	@Override
	protected IScanner createScanner(FileContent content, IScannerInfo scanInfo, IncludeFileContentProvider fcp,
			IParserLogService log) {
		return new ECPGScanner(content, scanInfo, getParserLanguage(), log, getScannerExtensionConfiguration(scanInfo),
				fcp);
	}

}
