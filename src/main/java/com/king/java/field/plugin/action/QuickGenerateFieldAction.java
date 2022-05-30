/*
 * Copyright (C) 2020 Jenly Yu
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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.king.java.field.plugin.component.ConfigComponent;
import org.apache.commons.lang.StringUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * @author <a href="mailto:yujinlin@mail.com">Jenly</a>
 */
public class QuickGenerateFieldAction extends GenerateFieldAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        String text = getClipboardText();
        if (StringUtils.isNotBlank(text)) {
            generateField(e, ConfigComponent.getInstance().getFieldParseConfig(), text, getCodeGenerator());
        }
    }

    /**
     * 获取剪切板内容
     *
     * @return
     */
    private String getClipboardText() {
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        // 获取剪切板中的内容
        Transferable transferable = sysClip.getContents(null);
        if (transferable != null) {
            // 检查内容是否是文本类型
            if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    return (String) transferable.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

}
