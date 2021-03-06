package net.winstone.pool;

public class Person {

	private String firstName;
	private String lastName;

	public Person() {
		super();
	}

	public Person(final String firstName, final String lastName) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName
	 *            the firstName to set
	 */
	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName
	 *            the lastName to set
	 */
	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Person [firstName=" + firstName + ", lastName=" + lastName + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((firstName == null) ? 0 : firstName.hashCode());
		result = (prime * result) + ((lastName == null) ? 0 : lastName.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return Boolean.TRUE;
		}
		if (obj == null) {
			return Boolean.FALSE;
		}
		if (getClass() != obj.getClass()) {
			return Boolean.FALSE;
		}
		final Person other = (Person) obj;
		if (firstName == null) {
			if (other.firstName != null) {
				return Boolean.FALSE;
			}
		} else if (!firstName.equals(other.firstName)) {
			return Boolean.FALSE;
		}
		if (lastName == null) {
			if (other.lastName != null) {
				return Boolean.FALSE;
			}
		} else if (!lastName.equals(other.lastName)) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

}
