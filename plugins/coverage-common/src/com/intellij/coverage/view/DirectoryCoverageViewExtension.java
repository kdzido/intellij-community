// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.coverage.view;

import com.intellij.coverage.BaseCoverageAnnotator;
import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageBundle;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DirectoryCoverageViewExtension extends CoverageViewExtension {
  protected final CoverageAnnotator myAnnotator;

  public DirectoryCoverageViewExtension(Project project,
                                        CoverageAnnotator annotator,
                                        CoverageSuitesBundle suitesBundle,
                                        CoverageViewManager.StateBean stateBean) {
    super(project, suitesBundle, stateBean);
    myAnnotator = annotator;
  }

  @Override
  public ColumnInfo[] createColumnInfos() {
    return new ColumnInfo[]{new ElementColumnInfo(),
      new PercentageCoverageColumnInfo(1, CoverageBundle.message("table.column.name.statistics"), mySuitesBundle, myStateBean)};
  }

  @Override
  public String getPercentage(int columnIdx, @NotNull AbstractTreeNode node) {
    final Object value = node.getValue();
    if (value instanceof PsiFile) {
      return myAnnotator.getFileCoverageInformationString((PsiFile)value, mySuitesBundle, myCoverageDataManager);
    }
    return value != null ? myAnnotator.getDirCoverageInformationString((PsiDirectory)value, mySuitesBundle, myCoverageDataManager) : null;
  }


  @Override
  public PsiElement getParentElement(PsiElement element) {
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      return containingFile.getContainingDirectory();
    }
    return null;
  }

  @NotNull
  @Override
  public AbstractTreeNode createRootNode() {
    VirtualFile baseDir = ProjectUtil.guessProjectDir(myProject);
    if (baseDir == null) {
      final VirtualFile[] roots = ProjectRootManager.getInstance(myProject).getContentRoots();
      baseDir = VfsUtil.getCommonAncestor(Arrays.asList(roots));
    }
    return new CoverageListRootNode(myProject, PsiManager.getInstance(myProject).findDirectory(baseDir), mySuitesBundle, myStateBean);
  }

  @Override
  public List<AbstractTreeNode<?>> getChildrenNodes(AbstractTreeNode node) {
    List<AbstractTreeNode<?>> children = new ArrayList<>();
    if (node instanceof CoverageListNode) {
      final Object val = node.getValue();
      if (val instanceof PsiFile || val == null) return Collections.emptyList();
      final PsiDirectory psiDirectory = (PsiDirectory)val;
      final PsiDirectory[] subdirectories = ReadAction.compute(() -> psiDirectory.getSubdirectories());
      for (PsiDirectory subdirectory : subdirectories) {
        if (myAnnotator.getDirCoverageInformationString(subdirectory, mySuitesBundle, myCoverageDataManager) == null) continue;
        CoverageListNode e = new CoverageListNode(myProject, subdirectory, mySuitesBundle, myStateBean);
        if (!e.getChildren().isEmpty()) {
          children.add(e);
        }
      }
      final PsiFile[] psiFiles = ReadAction.compute(() -> psiDirectory.getFiles());
      for (PsiFile psiFile : psiFiles) {
        if (myAnnotator.getFileCoverageInformationString(psiFile, mySuitesBundle, myCoverageDataManager) == null) continue;
        CoverageListNode e = new CoverageListNode(myProject, psiFile, mySuitesBundle, myStateBean);
        if (!myStateBean.isShowOnlyModified() || isModified(e.getFileStatus())) {
          children.add(e);
        }
        else {
          if (myAnnotator instanceof BaseCoverageAnnotator baseCoverageAnnotator) {
            baseCoverageAnnotator.setVcsFilteredChildren(true);
          }
        }
      }
    }
    return children;
  }

  @Override
  public boolean hasVCSFilteredNodes() {
    if (myAnnotator instanceof BaseCoverageAnnotator baseCoverageAnnotator) {
      return baseCoverageAnnotator.hasVcsFilteredChildren();
    }
    return super.hasVCSFilteredNodes();
  }
}
