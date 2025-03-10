package com.bgsoftware.superiorskyblock.database.loader.v1.attributes;

public final class WarpCategoryAttributes extends AttributesRegistry<WarpCategoryAttributes.Field> {

    public WarpCategoryAttributes() {
        super(Field.class);
    }

    @Override
    public WarpCategoryAttributes setValue(Field field, Object value) {
        return (WarpCategoryAttributes) super.setValue(field, value);
    }

    public enum Field {

        NAME,
        SLOT,
        ICON

    }

}
