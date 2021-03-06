package io.dropwizard.foundationdb.instrumented;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.DatabaseOptions;
import com.apple.foundationdb.ReadTransaction;
import com.apple.foundationdb.Transaction;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * A {@link MetricRegistry} instrumented FoundationDB {@link Database}. Instruments transactions with timers to record
 * timings and counts for database calls.
 */
public class InstrumentedDatabase implements Database {
    private final Database database;
    private final MetricRegistry metrics;
    private final String readMetricName;
    private final String readAsyncMetricName;
    private final String runMetricName;
    private final String runAsyncMetricName;

    public InstrumentedDatabase(final Database database, final MetricRegistry metrics, final String name) {
        this.database = database;
        this.metrics = metrics;
        this.readMetricName = MetricRegistry.name(name,"read.timeInNanos");
        this.readAsyncMetricName = MetricRegistry.name(name,"readAsync.timeInNanos");
        this.runMetricName = MetricRegistry.name(name,"run.timeInNanos");
        this.runAsyncMetricName = MetricRegistry.name(name,"runAsync.timeInNanos");
    }

    @Override
    public Transaction createTransaction() {
        return database.createTransaction();
    }

    @Override
    public Transaction createTransaction(final Executor e) {
        return database.createTransaction(e);
    }

    @Override
    public DatabaseOptions options() {
        return database.options();
    }

    @Override
    public <T> T read(final Function<? super ReadTransaction, T> retryable, final Executor e) {
        try (Timer.Context ignored = metrics.timer(readMetricName).time()) {
            return database.read(retryable, e);
        }
    }

    @Override
    public <T> CompletableFuture<T> readAsync(final Function<? super ReadTransaction, ? extends CompletableFuture<T>> retryable,
                                              final Executor executor) {
        final Timer.Context timerCtx = metrics.timer(readAsyncMetricName).time();
        return database.readAsync(retryable, executor).whenComplete((result, error) -> timerCtx.stop());
    }

    @Override
    public <T> T run(final Function<? super Transaction, T> retryable, final Executor e) {
        try (Timer.Context ignored = metrics.timer(runMetricName).time()) {
            return database.run(retryable, e);
        }
    }

    @Override
    public <T> CompletableFuture<T> runAsync(final Function<? super Transaction, ? extends CompletableFuture<T>> retryable,
                                             final Executor executor) {
        final Timer.Context timerCtx = metrics.timer(runAsyncMetricName).time();
        return database.runAsync(retryable, executor).whenComplete((T result, Throwable error) -> timerCtx.stop());
    }

    @Override
    public void close() {
        database.close();
    }

    @Override
    public Executor getExecutor() {
        return database.getExecutor();
    }
}
