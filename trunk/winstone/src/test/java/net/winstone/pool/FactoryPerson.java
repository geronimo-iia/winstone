package net.winstone.pool;

public class FactoryPerson implements ResourceFactory<Person> {

    private int i = 0;

    public FactoryPerson() {
        super();
    }

    @Override
    public Person create() {
        return new Person("n-" + i++, "l-" + i);
    }

    @Override
    public void destroy(Person resource) {
    }
}
