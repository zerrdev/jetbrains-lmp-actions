package dev.zerr.lmpactions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.nio.file.Path;
import java.util.Objects;

public class LmpExtractAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        String lmpContent;
        try {
            lmpContent = (String) CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            Messages.showErrorDialog(project, "Clipboard read error: " + ex.getMessage(), "LMP Extract");
            return;
        }
        if (lmpContent == null || lmpContent.isEmpty()) {
            Messages.showErrorDialog(project, "Clipboard is empty or does not contain text.", "LMP Extract");
            return;
        }

        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            Messages.showErrorDialog(project, "Cannot determine project base directory.", "LMP Extract");
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                LmpOperator operator = new LmpOperator();
                int count = operator.extract(lmpContent, Path.of(baseDir.getPath()));
                Messages.showInfoMessage(project, "Extracted " + count + " files to project root.", "LMP Extract");
            } catch (Exception ex) {
                Messages.showErrorDialog(project, "Extraction failed: " + ex.getMessage(), "LMP Extract");
            }
        });
    }
}
