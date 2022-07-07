/*
 * Copyright (C) 2020 Jenly Yu, https://github.com/jenly1314/JavaFieldGenerator
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.king.java.field.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.king.java.field.plugin.dialog.GenerateFieldDialog;
import com.king.java.field.plugin.generator.CodeGeneratorImpl;
import com.king.java.field.plugin.generator.ICodeGenerator;
import com.king.java.field.plugin.entity.FieldParseConfig;
import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="mailto:yujinlin@mail.com">Jenly</a>
 */
public class GenerateFieldAction extends AnAction {

    private AnActionEvent anActionEvent;

    private ICodeGenerator codeGenerator = new CodeGeneratorImpl();

    private GenerateFieldDialog.OnClickListener clickListener = new GenerateFieldDialog.OnClickListener() {

        @Override
        public void onGenerate(FieldParseConfig fieldParseConfig, String text) {
            generateField(anActionEvent, fieldParseConfig, text, codeGenerator);
        }

        @Override
        public void onCancel() {
            //nothing...
        }
    };

    protected void generateField(@NotNull AnActionEvent anActionEvent, @NotNull FieldParseConfig fieldParseConfig,@NotNull String text,@NotNull ICodeGenerator codeGenerator) {
        //获取当前编辑的文件
        PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            Messages.showMessageDialog("File can not be null!", "Error", Messages.getInformationIcon());
            return;
        }
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            Messages.showMessageDialog("Editor can not be null!", "Error", Messages.getInformationIcon());
            return;
        }

        Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            Messages.showMessageDialog("Project can not be null!", "Error", Messages.getInformationIcon());
            return;
        }

        // 获取当前编辑的class对象
        PsiElement psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());

        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
        if (psiClass == null) {
            Messages.showMessageDialog("Class can not be null!", "Error", Messages.getInformationIcon());
            return;
        }

        WriteCommandAction.runWriteCommandAction(anActionEvent.getProject(), () -> {
            try {
                codeGenerator.onGenerate(project, psiClass, fieldParseConfig, text);
            } catch (Exception e) {
                e.printStackTrace();
                Logger.getInstance(getClass()).error(e.getMessage());
            }
        });
    }


    public ICodeGenerator getCodeGenerator() {
        return codeGenerator;
    }

    public void setCodeGenerator(ICodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        this.anActionEvent = e;
        GenerateFieldDialog generateDialog = new GenerateFieldDialog();
        // 设置监听
        generateDialog.setOnClickListener(clickListener);
        // 自动调整对话框大小
        generateDialog.pack();
        // 设置对话框跟随当前windows窗口
        generateDialog.setLocationRelativeTo(WindowManager.getInstance().getFrame(e.getProject()));
        generateDialog.setVisible(true);
    }
}
