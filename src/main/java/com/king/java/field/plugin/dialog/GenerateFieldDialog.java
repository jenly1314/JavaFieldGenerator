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

import com.intellij.openapi.ui.Messages;
import com.king.java.field.plugin.component.ConfigComponent;
import com.king.java.field.plugin.entity.FieldParseConfig;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.util.Arrays;

import static com.king.java.field.plugin.dialog.GenerateFieldDialog.Modifier.*;

/**
 * 生成字段对话框
 *
 * @author <a href="mailto:yujinlin@mail.com">Jenly</a>
 */
public class GenerateFieldDialog extends JDialog {

    private JPanel panel;
    private JTextArea textArea;
    private JButton btnCancel, btnGenerate, btnSetting;
    private OnClickListener onClickListener;
    private JRadioButton rbPublic, rbProtected, rbPrivate;
    private JCheckBox cbGetAndSet;
    private JCheckBox cbToString;
    private JSpinner spField;
    private JSpinner spFieldType;
    private JSpinner spFieldComment;

    private FieldParseConfig fieldParseConfig;

    private static final int MINIMUM = 0;
    private static final int MAXIMUM = 9;

    public enum Modifier {
        /**
         * 公开
         */
        PUBLIC("public"),
        /**
         * 受保护
         */
        PROTECTED("protected"),
        /**
         * 私有
         */
        PRIVATE("private");

        private String value;

        Modifier(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Modifier ofValue(String value) {
            return Arrays.stream(values()).filter(it -> it.value.equals(value)).findFirst().get();
        }

    }

    /**
     * 成员变量可见性修饰符
     */
    private Modifier modifier;

    public GenerateFieldDialog() {
        setContentPane(panel);
        setModal(true);
        setTitle("Java Field");

        getRootPane().setDefaultButton(btnGenerate);

        fieldParseConfig = ConfigComponent.getInstance().getFieldParseConfig();
        modifier = ofValue(fieldParseConfig.getFieldModifier());
        switch (modifier) {
            case PUBLIC:
                rbPublic.setSelected(true);
                break;
            case PROTECTED:
                rbProtected.setSelected(true);
                break;
            case PRIVATE:
                rbPrivate.setSelected(true);
                break;
            default:
                break;
        }

        cbGetAndSet.setSelected(fieldParseConfig.isGenerateGetterAndSetter());
        cbToString.setSelected(fieldParseConfig.isGenerateToString());


        spField.addChangeListener(l -> {
            fieldParseConfig.setFieldColumn((int) spField.getValue());
        });

        spFieldType.addChangeListener(l -> {
            fieldParseConfig.setFieldTypeColumn((int) spFieldType.getValue());
        });

        spFieldComment.addChangeListener(l -> {
            fieldParseConfig.setFieldCommentColumn((int) spFieldComment.getValue());
        });

        rbPublic.addChangeListener(l -> {
            if (rbPublic.isSelected()) {
                modifier = PUBLIC;
                rbProtected.setSelected(false);
                rbPrivate.setSelected(false);
                fieldParseConfig.setFieldModifier(modifier.getValue());
            }
        });
        rbProtected.addChangeListener(l -> {
            if (rbProtected.isSelected()) {
                modifier = PROTECTED;
                rbPublic.setSelected(false);
                rbPrivate.setSelected(false);
                fieldParseConfig.setFieldModifier(modifier.getValue());
            }
        });
        rbPrivate.addChangeListener(l -> {
            if (rbPrivate.isSelected()) {
                modifier = PRIVATE;
                rbPublic.setSelected(false);
                rbProtected.setSelected(false);
                fieldParseConfig.setFieldModifier(modifier.getValue());
            }
        });

        cbGetAndSet.addChangeListener(l -> {
            fieldParseConfig.setGenerateGetterAndSetter(cbGetAndSet.isSelected());
        });
        cbToString.addChangeListener(l -> {
            fieldParseConfig.setGenerateToString(cbToString.isSelected());
        });

        btnGenerate.addActionListener(l -> {
            if (StringUtils.isNotBlank(textArea.getText())) {
                if (onClickListener != null) {
                    onClickListener.onGenerate(fieldParseConfig, textArea.getText());
                }
                dispose();
            } else {
                Messages.showMessageDialog("Text can not be null!", "Error", Messages.getInformationIcon());
            }

        });
        btnCancel.addActionListener(l -> {
            if (onClickListener != null) {
                onClickListener.onCancel();
            }
            dispose();
        });
        btnSetting.addActionListener(l -> {
            SettingFieldTypeDialog settingFieldTypeDialog = new SettingFieldTypeDialog();
            // 自动调整对话框大小
            settingFieldTypeDialog.pack();
            settingFieldTypeDialog.setLocationRelativeTo(this);
            settingFieldTypeDialog.setVisible(true);
        });

        spField.setModel(createSpinnerNumberModel());
        spFieldType.setModel(createSpinnerNumberModel());
        spFieldComment.setModel(createSpinnerNumberModel());

        spField.setValue(fieldParseConfig.getFieldColumn());
        spFieldType.setValue(fieldParseConfig.getFieldTypeColumn());
        spFieldComment.setValue(fieldParseConfig.getFieldCommentColumn());
    }

    private SpinnerNumberModel createSpinnerNumberModel(){
        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel();
        spinnerNumberModel.setMinimum(MINIMUM);
        spinnerNumberModel.setMaximum(MAXIMUM);
        return spinnerNumberModel;
    }

    /**
     * 点击监听器
     */
    public interface OnClickListener {
        /**
         * 生成
         *
         * @param fieldParseConfig
         * @param text
         */
        void onGenerate(FieldParseConfig fieldParseConfig, String text);

        /**
         * 取消
         */
        void onCancel();
    }

    /**
     * 设置监听器
     *
     * @param onClickListener
     */
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }
}
