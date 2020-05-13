package org.optaplanner.examples.cloudbalancing;

import java.util.function.Function;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sum;
import static org.optaplanner.core.api.score.stream.Joiners.equal;

public class CloudBalancingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                requiredCpuPowerTotal(constraintFactory),
                computerCost(constraintFactory)
        };
    }

    // ************************************************************************
    // Hard constraints
    // ************************************************************************

    private Constraint requiredCpuPowerTotal(ConstraintFactory constraintFactory) {
        return constraintFactory.from(CloudProcess.class)
                .groupBy(CloudProcess::getComputer, sum(CloudProcess::getRequiredCpuPower))
                .filter((computer, requiredCpuPower) -> requiredCpuPower > computer.getCpuPower())
                .penalize("requiredCpuPowerTotal",
                        HardSoftScore.ONE_HARD,
                        (computer, requiredCpuPower) -> requiredCpuPower - computer.getCpuPower());
    }

    // ************************************************************************
    // Soft constraints
    // ************************************************************************

    private Constraint computerCost(ConstraintFactory constraintFactory) {
        return constraintFactory.from(CloudComputer.class)
                .ifExists(CloudProcess.class, equal(Function.identity(), CloudProcess::getComputer))
                .penalize("computerCost",
                        HardSoftScore.ONE_SOFT,
                        CloudComputer::getCost);
    }
}
