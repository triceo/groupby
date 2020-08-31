package org.optaplanner.examples.cloudbalancing;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
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
import org.optaplanner.examples.cloudbalancing.domain.CloudBalance;
import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(jvmArgs = {"-Xms8G", "-Xmx8G"})
public class Main {

    private static final Random RANDOM = new Random(0);
    private static final KieSessionSupplier ACCUMULATE_DRL_SESSION_SUPPLIER =
            new DrlBasedKieBase("cloudBalancingScoreRules.drl");
    private static final KieSessionSupplier GROUPBY_DRL_SESSION_SUPPLIER =
            new DrlBasedKieBase("cloudBalancingScoreRules3.drl");
    private static final KieSessionSupplier GROUPBY_EXEC_MODEL_SESSION_SUPPLIER =
            new ExecModelBasedKieBase();
    @Param({"groupByDrl", "groupBy", "accumulate"})
    public String scoreDirectorFactoryType;
    private CloudBalance solutionToTest = readSolution("solved");
    private KieSession session;
    private CloudProcess entity1;
    private CloudProcess entity2;
    private FactHandle entity1fh;
    private FactHandle entity2fh;

    private static CloudBalance readSolution(String id) {
        SolutionIO<CloudBalance> solutionFileIO = new SolutionIO<>(CloudBalance.class);
        return solutionFileIO.read(new File("data/cloudbalancing/" + id + ".xml"));
    }

    private KieSession newKieSession() {
        switch (scoreDirectorFactoryType) {
            case "groupByDrl":
                return GROUPBY_DRL_SESSION_SUPPLIER.get();
            case "groupBy":
                return GROUPBY_EXEC_MODEL_SESSION_SUPPLIER.get();
            case "accumulate":
                return ACCUMULATE_DRL_SESSION_SUPPLIER.get();
            default:
                throw new IllegalStateException();
        }
    }

    @Setup(Level.Invocation)
    public void createSession() {
        session = newKieSession();
        solutionToTest.getProcessList()
                .forEach(session::insert);
        solutionToTest.getComputerList()
                .forEach(session::insert);
        session.fireAllRules();
        // Pick the changes to benchmark.
        List<CloudProcess> processes = solutionToTest.getProcessList();
        entity1 = processes.get(RANDOM.nextInt(processes.size()));
        entity1fh = session.getFactHandle(entity1);
        entity2 = entity1;
        entity2fh = session.getFactHandle(entity2);
        while (Objects.equals(entity2.getComputer(), entity1.getComputer())) {
            // Make sure we pick a process running on a different computer.
            entity2 = processes.get(RANDOM.nextInt(processes.size()));
        }
    }

    @TearDown(Level.Invocation)
    public void closeSession() {
        try {
            session.dispose(); // Close the session to prevent memory leaks.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Benchmark
    @Fork(1)
    @Warmup(iterations = 1)
    public Blackhole swapMove(Blackhole bh) {
        session.delete(entity1fh);
        CloudComputer originalComputer = entity1.getComputer();
        entity1.setComputer(entity2.getComputer());  // changes the fact
        session.insert(entity1);
        session.delete(entity2fh);
        entity2.setComputer(originalComputer);  // changes the fact
        session.insert(entity2);
        bh.consume(session.fireAllRules()); // fireAllRules()
        return bh;
    }

    @Benchmark
    @Fork(1)
    @Warmup(iterations = 1)
    public Blackhole changeMove(Blackhole bh) {
        session.delete(entity1fh);
        entity1.setComputer(entity2.getComputer()); // changes the fact
        session.insert(entity1);
        bh.consume(session.fireAllRules());
        return bh;
    }
}
