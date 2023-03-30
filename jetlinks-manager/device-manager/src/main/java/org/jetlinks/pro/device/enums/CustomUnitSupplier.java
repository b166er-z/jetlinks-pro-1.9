package org.jetlinks.pro.device.enums;

import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.unit.ValueUnitSupplier;
import org.jetlinks.core.metadata.unit.ValueUnits;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CustomUnitSupplier implements ValueUnitSupplier {

    public static void register() {
    }

    static {
        ValueUnits.register(new CustomUnitSupplier());
    }

    @Override
    public Optional<ValueUnit> getById(String id) {
        return Optional.ofNullable(CustomUnit.of(id));
    }

    @Override
    public List<ValueUnit> getAll() {
        return Arrays.asList(CustomUnit.values());
    }
}
