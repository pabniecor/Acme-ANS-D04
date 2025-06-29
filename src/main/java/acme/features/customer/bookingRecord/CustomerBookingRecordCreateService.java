
package acme.features.customer.bookingRecord;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.customer_management.Booking;
import acme.entities.customer_management.BookingRecord;
import acme.entities.customer_management.Passenger;
import acme.realms.Customer;

@GuiService
public class CustomerBookingRecordCreateService extends AbstractGuiService<Customer, BookingRecord> {

	@Autowired
	private CustomerBookingRecordRepository repository;


	@Override
	public void authorise() {
		boolean status = false;
		int customerId;
		int bookingId;
		Booking booking;
		Customer currentCustomer;

		currentCustomer = (Customer) super.getRequest().getPrincipal().getActiveRealm();
		customerId = currentCustomer.getId();

		if (super.getRequest().hasData("bookingId")) {
			bookingId = super.getRequest().getData("bookingId", int.class);
			booking = this.repository.findBookingById(bookingId);

			if (booking != null) {
				status = booking.getCustomer().getId() == customerId && booking.getDraftMode();

				if (status && super.getRequest().getMethod().equals("POST") && super.getRequest().hasData("passenger")) {
					int passengerId = super.getRequest().getData("passenger", int.class);

					if (passengerId != 0) {
						Passenger passenger = this.repository.findPassengerById(passengerId);

						if (passenger == null || passenger.getCustomer().getId() != customerId || passenger.getDraftModePassenger() == true)
							status = false;

						if (status) {
							Collection<Passenger> assignedPassengers = this.repository.findAssignedPassengersByBookingId(bookingId);
							if (assignedPassengers.contains(passenger))
								status = false;
						}
					}
				}
			}
		}

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		BookingRecord bookingRecord;
		Booking booking;
		int bookingId;

		bookingId = super.getRequest().getData("bookingId", int.class);
		booking = this.repository.findBookingById(bookingId);

		bookingRecord = new BookingRecord();
		bookingRecord.setBooking(booking);

		super.getBuffer().addData(bookingRecord);
	}

	@Override
	public void bind(final BookingRecord bookingRecord) {
		super.bindObject(bookingRecord, "passenger");
	}

	@Override
	public void validate(final BookingRecord bookingRecord) {
		;
	}

	@Override
	public void perform(final BookingRecord bookingRecord) {
		this.repository.save(bookingRecord);
	}

	@Override
	public void unbind(final BookingRecord bookingRecord) {
		Dataset dataset;
		Collection<Passenger> notAssignedPassengers;
		SelectChoices choicesPassengers;

		int customerId = super.getRequest().getPrincipal().getActiveRealm().getId();
		int bookingId = super.getRequest().getData("bookingId", int.class);

		notAssignedPassengers = this.repository.findNotAssignedPassengersByCustomerAndBookingId(customerId, bookingId);
		choicesPassengers = SelectChoices.from(notAssignedPassengers, "fullName", bookingRecord.getPassenger());

		dataset = super.unbindObject(bookingRecord, "passenger", "booking");
		dataset.put("passengers", choicesPassengers);

		super.getResponse().addData(dataset);
	}
}
