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
package com.king.java.field.plugin.dialog;

import com.king.java.field.plugin.component.ConfigComponent;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段类型配置模板对话框
 *
 * @author <a href="mailto:yujinlin@mail.com">Jenly</a>
 */
public class SettingFieldTypeDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea textArea;
    private JCheckBox cbArrayToList;

    public SettingFieldTypeDialog() {
        setContentPane(contentPane);
        setModal(true);

        setTitle("Java Field Type Setting");

        getRootPane().setDefaultButton(buttonOK);

        cbArrayToList.setSelected(ConfigComponent.getInstance().getFieldParseConfig().isConvertArrayToList());

        cbArrayToList.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                ConfigComponent.getInstance().getFieldParseConfig().setConvertArrayToList(cbArrayToList.isSelected());
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        final StringBuilder builder = new StringBuilder();
        ConfigComponent.getInstance().getFieldParseConfig().getFiledTypeConvertMap().forEach((key, value) -> {
            builder.append(key).append("=").append(value).append("\n");
        });
        textArea.setText(builder.toString());
    }

    private void onOK() {
        // add your code here
        saveSetting(textArea.getText());
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    private void saveSetting(String text){
        Map<String, String> filedTypeConvertMap = new LinkedHashMap<>(16);
        String[] strings = text.split("\n");
        if(strings != null){
            for (String string : strings) {
                if(StringUtils.isNotBlank(string)){
                    String[] kv = string.split("=");
                    if(kv.length >= 2){
                        filedTypeConvertMap.put(kv[0].trim(), kv[1].trim());
                    }else{
                        filedTypeConvertMap.put(kv[0].trim(), null);
                    }
                }
            }
        }

        ConfigComponent.getInstance().getFieldParseConfig().setFiledTypeConvertMap(filedTypeConvertMap);
    }

    public static void main(String[] args) {
        SettingFieldTypeDialog dialog = new SettingFieldTypeDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
