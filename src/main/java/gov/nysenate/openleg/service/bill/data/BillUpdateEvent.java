package gov.nysenate.openleg.service.bill.data;

import gov.nysenate.openleg.model.bill.Bill;

import java.time.LocalDateTime;

public class BillUpdateEvent
{
    protected Bill bill;
    protected LocalDateTime updateDateTime;

    /** --- Constructors --- */

    public BillUpdateEvent(Bill bill, LocalDateTime updateDateTime) {
        this.bill = bill;
        this.updateDateTime = updateDateTime;
    }

    /** --- Basic Getters/Setters --- */

    public Bill getBill() {
        return bill;
    }

    public LocalDateTime getUpdateDateTime() {
        return updateDateTime;
    }
}