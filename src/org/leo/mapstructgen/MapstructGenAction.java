package org.leo.mapstructgen;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MapstructGenAction extends AnAction {

    PsiImportList psiImportList = null;

    @Override
    public void actionPerformed(AnActionEvent event) {

        DataContext dataContext = event.getDataContext();
        PsiFile psiFile = event.getData(LangDataKeys.PSI_FILE);
        PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        if (psiElement instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) psiElement;
            PsiType returnType = psiMethod.getReturnType();


            PsiParameterList psiParameterList = psiMethod.getParameterList();
            PsiParameter[] psiParameters = psiParameterList.getParameters();
            if (psiParameters == null || psiParameters.length == 0) {
                Messages.showErrorDialog(event.getProject(), "选中的方法未包含任何入参", "错误");
                return;
            }
            if (psiParameters.length > 1) {
                Messages.showErrorDialog(event.getProject(), "仅支持单个入参的方法自动生成", "错误");
                return;
            }
            if (returnType instanceof PsiPrimitiveType) {
                Messages.showErrorDialog(event.getProject(), "方法返回值不能为void或私有类型", "错误");
                return;
            }
            psiFile.acceptChildren(new PsiElementVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {

                    if (element instanceof PsiImportList) {
                        psiImportList = (PsiImportList) element;
                    }

                }
            });
            PsiClass sourceClass = PsiTypesUtil.getPsiClass(psiParameters[0].getType());
            PsiClass targetClass = PsiTypesUtil.getPsiClass(returnType);
            List<String> sourceFieldNames = getFieldNames(sourceClass);
            List<String> targetFieldNames = getFieldNames(targetClass);
            sourceFieldNames = sortAndUnique(sourceFieldNames);
            targetFieldNames = sortAndUnique(targetFieldNames);


            GenUI.ApplyCallback applyCallback = new GenUI.ApplyCallback() {
                @Override
                public void apply(final Map<String, String> sourceToTargetMapping) {

                    WriteCommandAction.runWriteCommandAction(event.getProject(), new Runnable() {
                        @Override
                        public void run() {

                            PsiElementFactoryImpl psiElementFactory = new PsiElementFactoryImpl(event.getProject());
                            PsiImportStatement[] importStatements = psiImportList.getImportStatements();
                            boolean isMappingImport = false;
                            for (PsiImportStatement importStatement : importStatements) {
                                String name = importStatement.getQualifiedName();
                                if (name.equals("org.mapstruct.Mapping")) {
                                    isMappingImport = true;
                                }
                            }
                            if (!isMappingImport) {
                                PsiClass psiClass = JavaPsiFacade.getInstance(event.getProject()).findClass("org.mapstruct.Mapping", GlobalSearchScope.allScope(event.getProject()));
                                PsiImportStatement psiImportStatement = psiElementFactory.createImportStatement(psiClass);
                                psiImportList.add(psiImportStatement);
                            }
                            Set<String> sourceSet = sourceToTargetMapping.keySet();
                            for (String source : sourceSet) {
                                String target = sourceToTargetMapping.get(source);

                                PsiAnnotation annotationFromText =
                                        psiElementFactory.createAnnotationFromText("@Mapping(source = \"" + source + "\",target = \"" + target + "\")", psiFile);
                                psiMethod.getModifierList().add(annotationFromText);
                            }

                        }
                    });


                }
            };
            ShowSettingsUtil.getInstance().editConfigurable(event.getProject(), new GenUI(sourceClass.getName(), targetClass.getName(), sourceFieldNames, targetFieldNames, applyCallback));
        }

    }

    private List<String> sortAndUnique(List<String> fieldNames) {

        Set<String> tempSet = new HashSet<>();
        tempSet.addAll(fieldNames);
        List<String> result = new ArrayList<>();
        result.addAll(tempSet);
        Collections.sort(result);
        return result;
    }

    private List<String> getFieldNames(PsiClass psiClass) {

        List<String> fieldNames = new ArrayList<>();
        PsiField[] fields = psiClass.getAllFields();
        for (PsiField psiField : fields) {
            fieldNames.add(psiField.getName());
        }
        PsiClass superClass = psiClass.getSuperClass();
        if (!superClass.getName().equals("Object")) {
            fieldNames.addAll(getFieldNames(superClass));
        }
        return fieldNames;
    }
}
