package org.optaplanner.examples.cloudbalancing;

import java.io.File;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.stream.ConstraintStreamScoreDirectorFactory;
import org.optaplanner.core.impl.solver.DefaultSolverFactory;
import org.optaplanner.examples.cloudbalancing.domain.CloudBalance;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;
import org.optaplanner.examples.cloudbalancing.optional.score.CloudBalancingConstraintProvider;
import org.optaplanner.persistence.xstream.impl.domain.solution.XStreamSolutionFileIO;

@State(Scope.Benchmark)
@Fork(jvmArgs = {"-Xms8G", "-Xmx8G"})
public class Main {

    private static final SolutionDescriptor<CloudBalance> SOLUTION_DESCRIPTOR =
            SolutionDescriptor.buildSolutionDescriptor(CloudBalance.class, CloudProcess.class);
    private static final CloudBalance SOLUTION = readSolution();
    private static final InnerScoreDirectorFactory<CloudBalance> CS_SCORE_DIRECTOR_FACTORY =
            new ConstraintStreamScoreDirectorFactory<>(SOLUTION_DESCRIPTOR, new CloudBalancingConstraintProvider(),
                    ConstraintStreamImplType.DROOLS);
    private static final InnerScoreDirectorFactory<CloudBalance> DRL_SCORE_DIRECTOR_FACTORY =
            getDrlScoreDirectorFactory();

    private ScoreDirector<CloudBalance> constraintStreamScoreDirector;
    private ScoreDirector<CloudBalance> droolsScoreDirector;

    private static CloudBalance readSolution() {
        XStreamSolutionFileIO<CloudBalance> solutionFileIO = new XStreamSolutionFileIO(CloudBalance.class);
        return solutionFileIO.read(new File("data/cloudbalancing/unsolved/1600computers-4800processes.xml"));
    }

    private static InnerScoreDirectorFactory<CloudBalance> getDrlScoreDirectorFactory() {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource("org/optaplanner/examples/cloudbalancing/solver/cloudBalancingSolverConfig.xml");
        DefaultSolverFactory<CloudBalance> dsf = new DefaultSolverFactory<>(solverConfig);
        return (InnerScoreDirectorFactory<CloudBalance>) dsf.getScoreDirectorFactory();
    }

    private static ScoreDirector<CloudBalance> newScoreDirector(InnerScoreDirectorFactory<CloudBalance> scoreDirectorFactory) {
        return scoreDirectorFactory.buildScoreDirector(false, false);
    }

    @Setup(Level.Invocation)
    public void createSession() {
        constraintStreamScoreDirector = newScoreDirector(CS_SCORE_DIRECTOR_FACTORY);
        droolsScoreDirector = newScoreDirector(DRL_SCORE_DIRECTOR_FACTORY);
    }

    @TearDown(Level.Invocation)
    public void closeSession() {
        Stream.of(constraintStreamScoreDirector, droolsScoreDirector)
                .forEach(sd -> {
                    try {
                        sd.close();
                    } catch (NullPointerException ex) {
                        // Session for one of the SDs may not be created at this point. Ignore, not a problem.
                    }
                });
    }

    @Benchmark
    public Blackhole drl(Blackhole bh) {
        // See DRL in src/main/resources/org/optaplanner/examples/cloudbalancing/solver/cloudBalancingScoreRules.drl.
        droolsScoreDirector.setWorkingSolution(SOLUTION); // insert all facts
        bh.consume(droolsScoreDirector.calculateScore()); // fireAllRules()
        return bh;
    }

    @Benchmark
    public Blackhole constraintStream(Blackhole bh) {
        // See CloudBalancingConstraintProvider class.
        constraintStreamScoreDirector.setWorkingSolution(SOLUTION); // insert all facts
        bh.consume(constraintStreamScoreDirector.calculateScore()); // fireAllRules()
        return bh;
    }
}
