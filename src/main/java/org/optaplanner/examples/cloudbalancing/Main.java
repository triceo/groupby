package org.optaplanner.examples.cloudbalancing;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.stream.ConstraintStreamScoreDirectorFactory;
import org.optaplanner.core.impl.solver.DefaultSolverFactory;
import org.optaplanner.examples.cloudbalancing.domain.CloudBalance;
import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;
import org.optaplanner.examples.cloudbalancing.optional.score.CloudBalancingConstraintProvider;
import org.optaplanner.persistence.xstream.impl.domain.solution.XStreamSolutionFileIO;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(jvmArgs = {"-Xms8G", "-Xmx8G"})
public class Main {

    private static final Random RANDOM = new Random(0);
    private static final SolutionDescriptor<CloudBalance> SOLUTION_DESCRIPTOR =
            SolutionDescriptor.buildSolutionDescriptor(CloudBalance.class, CloudProcess.class);
    private static final InnerScoreDirectorFactory<CloudBalance> CS_SCORE_DIRECTOR_FACTORY =
            new ConstraintStreamScoreDirectorFactory<>(SOLUTION_DESCRIPTOR, new CloudBalancingConstraintProvider(),
                    ConstraintStreamImplType.DROOLS);
    private static final InnerScoreDirectorFactory<CloudBalance> DRL_SCORE_DIRECTOR_FACTORY =
            getDrlScoreDirectorFactory();

    private static CloudBalance readSolution(String id) {
        XStreamSolutionFileIO<CloudBalance> solutionFileIO = new XStreamSolutionFileIO(CloudBalance.class);
        return solutionFileIO.read(new File("data/cloudbalancing/" + id + ".xml"));
    }

    private static InnerScoreDirectorFactory<CloudBalance> getDrlScoreDirectorFactory() {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource("org/optaplanner/examples/cloudbalancing/solver/cloudBalancingSolverConfig.xml");
        DefaultSolverFactory<CloudBalance> dsf = new DefaultSolverFactory<>(solverConfig);
        return (InnerScoreDirectorFactory<CloudBalance>) dsf.getScoreDirectorFactory();
    }

    private ScoreDirector<CloudBalance> newScoreDirector() {
        InnerScoreDirectorFactory<CloudBalance> scoreDirectorFactory;
        switch (scoreDirectorFactoryType) {
            case "DRL":
                // See DRL in src/main/resources/org/optaplanner/examples/cloudbalancing/solver/cloudBalancingScoreRules.drl.
                scoreDirectorFactory = DRL_SCORE_DIRECTOR_FACTORY;
                break;
            case "CS-D":
                // See CloudBalancingConstraintProvider class.
                scoreDirectorFactory = CS_SCORE_DIRECTOR_FACTORY;
                break;
            default:
                throw new IllegalStateException();
        }
        return scoreDirectorFactory.buildScoreDirector(false, constraintMatchEnabled);
    }

    @Param({"true", "false"})
    public boolean constraintMatchEnabled;

    @Param({"CS-D", "DRL"})
    public String scoreDirectorFactoryType;

    private CloudBalance solutionToTest = readSolution("solved");
    private ScoreDirector<CloudBalance> scoreDirector;
    private CloudProcess entity1;
    private CloudProcess entity2;

    @Setup(Level.Invocation)
    public void createSession() {
        scoreDirector = newScoreDirector();
        // Initialize data.
        scoreDirector.setWorkingSolution(solutionToTest);
        scoreDirector.calculateScore(); // fireAllRules()
        // Pick the changes to benchmark.
        List<CloudProcess> processes = solutionToTest.getProcessList();
        entity1 = processes.get(RANDOM.nextInt(processes.size()));
        entity2 = entity1;
        while (Objects.equals(entity2.getComputer(), entity1.getComputer())) {
            // Make sure we pick a process running on a different computer.
            entity2 = processes.get(RANDOM.nextInt(processes.size()));
        }
    }

    @TearDown(Level.Invocation)
    public void closeSession() {
        scoreDirector.close(); // Close the session to prevent memory leaks.
    }

    @Benchmark
    @Fork(10)
    @Warmup(iterations = 10)
    public Blackhole swapMove(Blackhole bh) {
        scoreDirector.beforeVariableChanged(entity1, "computer");
        CloudComputer originalComputer = entity1.getComputer();
        entity1.setComputer(entity2.getComputer());  // changes the fact
        scoreDirector.afterVariableChanged(entity1, "computer"); // updates the fact in the WM
        scoreDirector.beforeVariableChanged(entity2, "computer");
        entity2.setComputer(originalComputer);  // changes the fact
        scoreDirector.afterVariableChanged(entity2, "computer"); // updates the fact in the WM
        scoreDirector.triggerVariableListeners(); // required by optaplanner; doesn't do anything in this case
        bh.consume(scoreDirector.calculateScore()); // fireAllRules()
        return bh;
    }

    @Benchmark
    @Fork(10)
    @Warmup(iterations = 10)
    public Blackhole changeMove(Blackhole bh) {
        scoreDirector.beforeVariableChanged(entity1, "computer");
        entity1.setComputer(entity2.getComputer()); // changes the fact
        scoreDirector.afterVariableChanged(entity1, "computer"); // updates the fact in the WM
        scoreDirector.triggerVariableListeners(); // required by optaplanner; doesn't do anything in this case
        bh.consume(scoreDirector.calculateScore()); // fireAllRules()
        return bh;
    }

}
