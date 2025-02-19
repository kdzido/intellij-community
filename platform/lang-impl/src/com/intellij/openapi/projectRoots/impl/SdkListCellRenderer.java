// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class SdkListCellRenderer extends ColoredListCellRenderer<Sdk> {
  private final @NlsContexts.Label String myNullText;
  private final boolean myShowHomePath;

  public SdkListCellRenderer(@NotNull @NlsContexts.Label String nullText) {
    this(nullText, false);
  }

  public SdkListCellRenderer(@NotNull @NlsContexts.Label String nullText, boolean showHomePath) {
    myNullText = nullText;
    myShowHomePath = showHomePath;
  }

  @Override
  protected void customizeCellRenderer(@NotNull JList<? extends Sdk> list, Sdk sdk, int index, boolean selected, boolean hasFocus) {
    if (sdk != null) {
      setIcon(getSdkIcon(sdk));
      append(sdk.getName());
      if (myShowHomePath) {
        append(" (" + FileUtil.toSystemDependentName(StringUtil.notNullize(sdk.getHomePath())) + ")",
               selected ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
    }
    else {
      append(myNullText);
    }
  }

  private Icon getSdkIcon(Sdk sdk) {
    return ((SdkType) sdk.getSdkType()).getIcon();
  }
}