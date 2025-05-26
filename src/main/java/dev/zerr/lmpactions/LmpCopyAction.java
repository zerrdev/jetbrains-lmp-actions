package dev.zerr.lmpactions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class LmpCopyAction extends AnAction {

    private static final List<String> DEFAULT_EXCLUDE_EXTENSIONS = Arrays.asList(
            ".exe", ".dll", ".so", ".bin", ".class", ".jar", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".mp3", ".mp4", ".wav", ".zip", ".tar", ".gz", ".7z", ".rar", ".iso"
    );
    private static final List<Pattern> DEFAULT_EXCLUDE_PATTERNS = Arrays.asList(
            Pattern.compile("^node_modules/"), Pattern.compile("^\\.git/"), Pattern.compile("^\\.idea/"), Pattern.compile("^\\.vscode/"), Pattern.compile("^__pycache__/")
    );

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (files == null || files.length == 0) {
            return;
        }

        LmpOperator operator = new LmpOperator();
        StringBuilder lmpContent = new StringBuilder();
        Path projectRoot = Path.of(project.getBasePath());

        try {
            for (VirtualFile vf : files) {
                Path path = Path.of(vf.getPath());
                if (vf.isDirectory()) {
                    lmpContent.append(operator.copyFolderAsLmp(path, DEFAULT_EXCLUDE_EXTENSIONS, DEFAULT_EXCLUDE_PATTERNS, projectRoot));
                } else if (vf.isValid() && !vf.isDirectory()) {
                    lmpContent.append(operator.copyFileAsLmp(path, projectRoot));
                }
            }
            CopyPasteManager.getInstance().setContents(new StringSelection(lmpContent.toString()));
            // Notification or popup can be added here
        } catch (Exception ex) {
            // Error handling (could use Notifications)
        }
    }
}
