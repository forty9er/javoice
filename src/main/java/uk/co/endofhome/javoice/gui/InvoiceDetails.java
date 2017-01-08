package uk.co.endofhome.javoice.gui;

import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Sequence;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import uk.co.endofhome.javoice.Observable;
import uk.co.endofhome.javoice.Observer;
import uk.co.endofhome.javoice.customer.Customer;
import uk.co.endofhome.javoice.invoice.ItemLine;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.totallylazy.Option.none;
import static com.googlecode.totallylazy.Option.option;
import static com.googlecode.totallylazy.Sequences.sequence;
import static uk.co.endofhome.javoice.gui.UiController.mainMenuStackPane;
import static uk.co.endofhome.javoice.invoice.Invoice.MAX_ITEM_LINES;

public class InvoiceDetails extends JavoiceScreen implements GuiObservable, Observable {
    StackPane invoiceDetailsStackPane;
    private GuiObserver guiObserver;
    private Observer observer;
    private Customer customer;
    private TextField nameField;
    private TextField orderNumberField;
    private TextField addressOneField;
    private TextField addressTwoField;
    private TextField postcodeField;
    private List<TextField> quantityFieldList;
    private List<SimpleDoubleProperty> quantityPropertyList;
    private List<TextField> descriptionFieldList;
    private List<TextField> unitPriceFieldList;
    private List<Label> totalLabelList;

    public InvoiceDetails(Option<Customer> customer) {
        this.customer = ensureCustomer(customer);
        initialise();
    }

    private Customer ensureCustomer(Option<Customer> customer) {
        if (customer.isDefined()) {
            return this.customer = customer.get();
        }
        return this.customer = new FakeCustomer();
    }

    private void initialise() {
        GridPane invoiceDetailsGrid = new GridPane();
        basicGridSetup(invoiceDetailsGrid, "Invoice details:", 1);

        Label nameLabel = initLabel(invoiceDetailsGrid, "Name:", 0, 3);
        nameField = initTextField(invoiceDetailsGrid, 3, customer.name, 0, 4);

        Label addressOne = initLabel(invoiceDetailsGrid, "Address (1):", 0, 5);
        addressOneField = initTextField(invoiceDetailsGrid, 4, customer.addressOne, 0, 6);

        Label addressTwo = initLabel(invoiceDetailsGrid, "Address (2):", 0, 7);
        addressTwoField = initTextField(invoiceDetailsGrid, 3, customer.addressTwo, 0, 8);

        Label postcodeLabel = initLabel(invoiceDetailsGrid, "Postcode:", 3, 7);
        postcodeField = initTextField(invoiceDetailsGrid, 1, customer.postcode, 3, 8);

        Label dateLabel = initLabel(invoiceDetailsGrid, "Date:", 5, 3);
        TextField dateField = initTextField(invoiceDetailsGrid, 1, todaysDate(), 5, 4);
        dateField.setDisable(true);

        Label orderNumberLabel = initLabel(invoiceDetailsGrid, "Order Number:", 5, 5);
        orderNumberField = initTextField(invoiceDetailsGrid, 1, "", 5, 6);

        Label accountCodeLabel = initLabel(invoiceDetailsGrid, "Account code:", 5, 7);
        TextField accountCodeField = initTextField(invoiceDetailsGrid, 1, customer.accountCode, 5, 8);
        accountCodeField.setDisable(true);

        Label quantity = initLabel(invoiceDetailsGrid, "Quantity", 0, 13);
        Label description = initLabel(invoiceDetailsGrid, "Description", 1, 13);
        Label unitPrice = initLabel(invoiceDetailsGrid, "Unit price", 4, 13);
        Label total = initLabel(invoiceDetailsGrid, "Total", 5, 13);

        quantityFieldList = new ArrayList<>();
        quantityPropertyList = new ArrayList<>();
        for (int i = 0; i < MAX_ITEM_LINES; i++) {
            SimpleDoubleProperty quantityPropertyForLine = new SimpleDoubleProperty();
            quantityPropertyList.add(quantityPropertyForLine);
            TextField quantityFieldForLine = new TextField();
            int i2 = i;
            quantityFieldForLine.textProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        Double val;
                        try {
                            val = new Double(newValue);
                        } catch(NumberFormatException e) {
                            val = 0d;
                        }
                        // TODO: blows up if number too large (over limit for Double?)
                        quantityPropertyList.get(i2).setValue( val );
                    }
            );
            quantityFieldList.add(quantityFieldForLine);
            quantityFieldList.get(i).setMaxWidth(75);
        }

        descriptionFieldList = new ArrayList<>();
        for (int i = 0; i < MAX_ITEM_LINES; i++) {
            descriptionFieldList.add(new TextField());
            descriptionFieldList.get(i).setMinWidth(200);
            GridPane.setColumnSpan(descriptionFieldList.get(i), 3);
        }

        unitPriceFieldList = new ArrayList<>();
        for (int i = 0; i < MAX_ITEM_LINES; i++) {
            unitPriceFieldList.add(new TextField());
            unitPriceFieldList.get(i).setMaxWidth(75);
        }

        totalLabelList = new ArrayList<>();
        for (int i = 0; i < MAX_ITEM_LINES; i++) {
            SimpleDoubleProperty unitPricePropertyForLine = new SimpleDoubleProperty();
            unitPricePropertyForLine.set(10);
            NumberBinding totalForLine = quantityPropertyList.get(i).multiply(unitPricePropertyForLine);
            Label totalLabelForLine = new Label();
            totalForLine.addListener(
                    (observable, oldValue, newValue) -> totalLabelForLine.setText(newValue.toString()));
            totalLabelList.add(totalLabelForLine);
        }

        for (int i = 0; i < MAX_ITEM_LINES; i++) {
            invoiceDetailsGrid.add(quantityFieldList.get(i), 0, 14 + i);
            invoiceDetailsGrid.add(descriptionFieldList.get(i), 1, 14 + i);
            invoiceDetailsGrid.add(unitPriceFieldList.get(i), 4, 14 + i);
            invoiceDetailsGrid.add(totalLabelList.get(i), 5, 14 + i);
        }

        Button mainMenu = initButton(invoiceDetailsGrid, "Main menu", event -> notifyGuiObserver(mainMenuStackPane), 0, 31);

        Button submitInvoice = initButton(invoiceDetailsGrid, "Submit", event -> {
            try {
                newInvoice();
            } catch (IOException e) {
                // TODO: fix this mess too. should be throwing this exception somewhere, not swallowing it.
            }
        }, 2, 31);

        ScrollPane invoiceDetailsScroll = new ScrollPane(invoiceDetailsGrid);
        invoiceDetailsScroll.setFitToWidth(true);
        invoiceDetailsStackPane = new StackPane(invoiceDetailsScroll);
        // TODO: this doesn't work, for some reason:
        quantityFieldList.get(0).requestFocus();
    }

    private void newInvoice() throws IOException {
        Customer customerFromUI = updatedCustomer();
        Sequence<ItemLine> itemLines = sequence();
        for (int i = 0; i < MAX_ITEM_LINES; i++) {
            ItemLine itemLine = new ItemLine(
                    doubleOptionOrNone(quantityFieldList.get(i).getText()),
                    option(descriptionFieldList.get(i).getText()),
                    doubleOptionOrNone(unitPriceFieldList.get(i).getText())
            );
            itemLines = itemLines.append(itemLine);
        }
        observer.newInvoice(customerFromUI, orderNumberField.getText(), itemLines);
    }

    private Option<Double> doubleOptionOrNone(String text) {
        if (text.equals("")) {
            return none();
        }
        return option(Double.valueOf(text));
    }

    private Customer updatedCustomer() {
        return new Customer(
                nameField.getText(),
                addressOneField.getText(),
                addressTwoField.getText(),
                postcodeField.getText(),
                customer.phoneNumber,
                customer.accountCode
        );
    }

    private String todaysDate() {
        LocalDate now = LocalDate.now();
        DateTimeFormatter ukFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return now.format(ukFormat);
    }

    @Override
    public void registerGuiObserver(GuiObserver guiObserver) {
        this.guiObserver = guiObserver;
    }

    @Override
    public void notifyGuiObserver(StackPane stackPane) {
        guiObserver.switchScene(stackPane);
    }

    @Override
    public void registerObserver(Observer observer) {
        this.observer = observer;
    }

    // TODO: method/s not required, side-effect of the fact that the observer pattern stuff isn't quite the right tool for the job?
    @Override
    public void newCustomer(String name, String addressOne, String addressTwo, String postcode, String phoneNumber) throws IOException {
    }

    @Override
    public void searchForCustomer(String name) throws Exception {
    }
}
