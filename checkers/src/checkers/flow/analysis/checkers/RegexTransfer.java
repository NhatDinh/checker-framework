package checkers.flow.analysis.checkers;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.cfg.node.ClassNameNode;
import checkers.flow.cfg.node.IntegerLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.MethodAccessNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.util.FlowExpressionParseUtil;
import checkers.flow.util.FlowExpressionParseUtil.FlowExpressionContext;
import checkers.flow.util.FlowExpressionParseUtil.FlowExpressionParseException;
import checkers.regex.RegexAnnotatedTypeFactory;
import checkers.regex.quals.Regex;

public class RegexTransfer extends
        CFAbstractTransfer<CFValue, CFStore, RegexTransfer> {

    /** Like super.analysis, but more specific type. */
    protected RegexAnalysis analysis;

    public RegexTransfer(RegexAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
    }

    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(
                n, in);

        // refine result for the RegexUtil.isRegex(s, groups) method
        MethodAccessNode target = n.getTarget();
        ExecutableElement method = target.getMethod();
        Node receiver = target.getReceiver();
        if (receiver instanceof ClassNameNode) {
            ClassNameNode cn = (ClassNameNode) receiver;
            String receiverName = cn.getElement().toString();
            // correct method?
            if (receiverName.equals("checkers.regex.RegexUtil")
                    && method.toString()
                            .equals("isRegex(java.lang.String,int)")) {
                CFStore thenStore = result.getThenStore();
                FlowExpressionContext context = FlowExpressionParseUtil
                        .buildFlowExprContextForUse(n);
                try {
                    Receiver firstParam = FlowExpressionParseUtil.parse("#1",
                            context);
                    // add annotation with correct group count (if possible,
                    // regex annotation without count otherwise)
                    Node count = n.getArgument(1);
                    if (count instanceof IntegerLiteralNode) {
                        IntegerLiteralNode iln = (IntegerLiteralNode) count;
                        Integer groupCount = iln.getValue();
                        RegexAnnotatedTypeFactory f = (RegexAnnotatedTypeFactory) analysis.factory;
                        AnnotationMirror regexAnnotation = f.createRegexAnnotation(groupCount);
                        thenStore.insertValue(firstParam, regexAnnotation);
                    } else {
                        AnnotationMirror regexAnnotation = analysis.factory
                                .annotationFromClass(Regex.class);
                        thenStore.insertValue(firstParam, regexAnnotation);
                    }
                    //f.createRegexAnnotation();
                } catch (FlowExpressionParseException e) {
                    assert false;
                }
            }
        }

        return result;
    };
}
