package com.palantir.atlasdb.table.description.generated;

import java.util.List;

import javax.annotation.Generated;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.palantir.atlasdb.keyvalue.api.Namespace;
import com.palantir.atlasdb.table.generation.Triggers;
import com.palantir.atlasdb.transaction.api.Transaction;

@Generated("com.palantir.atlasdb.table.description.render.TableFactoryRenderer")
public final class GenericTestSchemaTableFactory {
    private final static Namespace defaultNamespace = Namespace.create("default", Namespace.UNCHECKED_NAME);
    private final List<Function<? super Transaction, SharedTriggers>> sharedTriggers;
    private final Namespace namespace;

    public static GenericTestSchemaTableFactory of(List<Function<? super Transaction, SharedTriggers>> sharedTriggers, Namespace namespace) {
        return new GenericTestSchemaTableFactory(sharedTriggers, namespace);
    }

    public static GenericTestSchemaTableFactory of(List<Function<? super Transaction, SharedTriggers>> sharedTriggers) {
        return new GenericTestSchemaTableFactory(sharedTriggers, defaultNamespace);
    }

    private GenericTestSchemaTableFactory(List<Function<? super Transaction, SharedTriggers>> sharedTriggers, Namespace namespace) {
        this.sharedTriggers = sharedTriggers;
        this.namespace = namespace;
    }

    public static GenericTestSchemaTableFactory of(Namespace namespace) {
        return of(ImmutableList.<Function<? super Transaction, SharedTriggers>>of(), namespace);
    }

    public static GenericTestSchemaTableFactory of() {
        return of(ImmutableList.<Function<? super Transaction, SharedTriggers>>of(), defaultNamespace);
    }

    public TableATable getTableATable(Transaction t, TableATable.TableATrigger... triggers) {
        return TableATable.of(t, namespace, Triggers.getAllTriggers(t, sharedTriggers, triggers));
    }

    public TableBTable getTableBTable(Transaction t, TableBTable.TableBTrigger... triggers) {
        return TableBTable.of(t, namespace, Triggers.getAllTriggers(t, sharedTriggers, triggers));
    }

    public interface SharedTriggers extends
            TableATable.TableATrigger,
            TableBTable.TableBTrigger {
        /* empty */
    }

    public abstract static class NullSharedTriggers implements SharedTriggers {
        @Override
        public void putTableA(Multimap<TableATable.TableARow, ? extends TableATable.TableANamedColumnValue<?>> newRows) {
            // do nothing
        }

        @Override
        public void putTableB(Multimap<TableBTable.TableBRow, ? extends TableBTable.TableBColumnValue> newRows) {
            // do nothing
        }
    }
}