package plugin.ecpg;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.core.formatter.DefaultCodeFormatterOptions;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.formatter.CodeFormatterVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.TextEdit;

@SuppressWarnings("restriction")
public class ECPGCodeFormatter extends CodeFormatter {

	private DefaultCodeFormatterOptions preferences;

	public ECPGCodeFormatter() {
		preferences = DefaultCodeFormatterOptions.getDefaultSettings();
	}

	@Override
	public TextEdit format(int kind, String source, int offset, int length, int indentationLevel,
			String lineSeparator) {

		FileContent content = FileContent.create("<text>", source.toCharArray());
		IScannerInfo scanInfo = new ScannerInfo();
		IncludeFileContentProvider includes = IncludeFileContentProvider.getSavedFilesProvider();
		IParserLogService log = new DefaultLogService();
		IASTTranslationUnit ast;
		try {
			ast = new ECPGLanguage().getASTTranslationUnit(content, scanInfo, includes, null, 0, log, true);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}

		preferences.initial_indentation_level = indentationLevel;
		preferences.line_separator = System.getProperty("line.separator");
		CodeFormatterVisitor visitor = new CodeFormatterVisitor(preferences, offset, length);

		return visitor.format(source, ast);
	}

	@Override
	public void setOptions(Map<String, ?> options) {
		if (options != null) {
			Map<String, String> formatterPrefs = new HashMap<>(options.size());
			for (String key : options.keySet()) {
				Object value = options.get(key);
				if (value instanceof String) {
					formatterPrefs.put(key, (String) value);
				}
			}
			preferences = new DefaultCodeFormatterOptions(formatterPrefs);
		} else {
			preferences = DefaultCodeFormatterOptions.getDefaultSettings();
		}
	}
}
