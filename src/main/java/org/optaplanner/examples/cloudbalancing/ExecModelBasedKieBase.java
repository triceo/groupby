package org.optaplanner.examples.cloudbalancing;

import org.drools.core.base.accumulators.IntegerSumAccumulateFunction;
import org.drools.model.Model;
import org.drools.model.PatternDSL;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.model.view.ViewItem;
import org.drools.modelcompiler.builder.KieBaseBuilder;
import org.drools.modelcompiler.dsl.pattern.D;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;

public final class ExecModelBasedKieBase implements KieSessionSupplier {

    private final KieBase cache;

    public ExecModelBasedKieBase() {
        // The primary pattern.
        Variable<CloudProcess> processVar = PatternDSL.declarationOf(CloudProcess.class);
        Variable<Integer> sumOnVar = PatternDSL.declarationOf(Integer.class);
        PatternDSL.PatternDef<CloudProcess> process = PatternDSL.pattern(processVar)
                .expr(p -> p.getComputer() != null)
                .bind(sumOnVar, CloudProcess::getRequiredCpuPower);

        // The groupBy pattern.
        Variable<CloudComputer> groupKeyVar = PatternDSL.declarationOf(CloudComputer.class);
        Variable<Integer> cpuSumVar = PatternDSL.declarationOf(Integer.class);
        ViewItem groupBy = PatternDSL.groupBy(process, processVar, groupKeyVar, CloudProcess::getComputer,
                D.accFunction(IntegerSumAccumulateFunction::new, sumOnVar).as(cpuSumVar));

        // The filter after groupBy
        ViewItem finalFilter = D.pattern(cpuSumVar)
                .expr("someExpr", groupKeyVar, (sum, computer) -> computer.getCpuPower() < sum);

        Rule rule = D.rule("requiredCpuPowerTotal")
                .build(groupBy, finalFilter,
                        D.on(groupKeyVar, cpuSumVar)
                                .execute((cloudComputer, cpuSum) -> {
                                    // Dummy operation.
                                }));
        Model model = new ModelImpl().addRule(rule);
        cache = KieBaseBuilder.createKieBaseFromModel(model);
    }

    @Override
    public KieSession get() {
        return cache.newKieSession();
    }
}
