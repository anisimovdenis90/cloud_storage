package services;

public enum DBPropertyNames {

    DRIVER("server.db.driver"),
    URL("server.db.url"),
    USERNAME("server.db.username"),
    PASSWORD("server.db.password"),
    MAXIMUM_POOL_SIZE("server.db.maximumPoolSize");

    public String name;

    DBPropertyNames(String name) {
        this.name = name;
    }
}
