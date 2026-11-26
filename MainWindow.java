package com.busticket.desktop;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {
    private final Connection connection;
    private final JTabbedPane tabs = new JTabbedPane();
    private boolean adminOpen;

    public MainWindow(Connection connection) {
        this.connection=connection;
        setTitle("Bus Ticket Manager | Bus Booking and Management");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1020,720)); setSize(1220,820); setLocationRelativeTo(null);
        getContentPane().setBackground(BusTicketApp.PALE); setLayout(new BorderLayout());
        JPanel top=new JPanel(new BorderLayout()); top.setBackground(BusTicketApp.INK); top.setBorder(new EmptyBorder(15,22,15,22));
        JLabel brand=new JLabel("BUS TICKET MANAGER    |    Bus Booking and Management"); brand.setForeground(Color.WHITE); brand.setFont(new Font("Segoe UI",Font.BOLD,21));
        JButton admin=BusTicketApp.button("Admin Login"); admin.addActionListener(e->openAdmin()); top.add(brand,BorderLayout.LINE_START);top.add(admin,BorderLayout.LINE_END);
        add(top,BorderLayout.NORTH); tabs.setFont(new Font("Segoe UI",Font.BOLD,14));
        tabs.addTab("Book a Trip",new BookingPanel(connection)); tabs.addTab("Find a Booking",new LookupPanel(connection)); add(tabs,BorderLayout.CENTER);
    }
    private void openAdmin() {
        if(adminOpen){tabs.setSelectedIndex(2);return;}
        JPasswordField password=new JPasswordField();
        int choice=JOptionPane.showConfirmDialog(this,password,"Enter the administrator password",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(choice!=JOptionPane.OK_OPTION)return;
        String expected=Database.setting("APP_ADMIN_PASSWORD","change-this-admin-password");
        if(!Arrays.equals(password.getPassword(),expected.toCharArray())){JOptionPane.showMessageDialog(this,"Incorrect password.","Access Denied",JOptionPane.WARNING_MESSAGE);return;}
        tabs.addTab("Admin Panel",new AdminPanel(connection));adminOpen=true;tabs.setSelectedIndex(2);
    }
}

record CityOption(long id,String name) { @Override public String toString(){return name;} }
record TripOption(long id,String origin,String destination,String bus,String type,Timestamp departure,Timestamp arrival,BigDecimal fare,int seats,int available) {
    String time(Timestamp value){return value.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy"));}
    @Override public String toString(){return time(departure)+"   |   "+origin+" ← "+destination+"   |   "+bus+" ("+type+")   |   "+fare.stripTrailingZeros().toPlainString()+" SDG   |   Available "+available+"/"+seats;}
}

class BookingPanel extends JPanel {
    private final Connection db;
    private final JComboBox<CityOption> origin=new JComboBox<>(), destination=new JComboBox<>();
    private final JSpinner date=new JSpinner(new SpinnerDateModel(java.util.Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()),null,null,Calendar.DAY_OF_MONTH));
    private final DefaultListModel<TripOption> tripModel=new DefaultListModel<>();
    private final JList<TripOption> tripList=new JList<>(tripModel);
    private final JPanel seatsPanel=new JPanel(new GridBagLayout());
    private final LinkedHashMap<Long,SeatButton> picked=new LinkedHashMap<>();
    private final JLabel fareLabel=new JLabel("Total: 0 SDG"), seatLabel=new JLabel("Selected Seats: 0");
    private final JTextField passenger=new JTextField(), phone=new JTextField(), email=new JTextField();
    private TripOption selectedTrip;
    private static final DateTimeFormatter SQL_DATE=DateTimeFormatter.ofPattern("yyyy-MM-dd");

    BookingPanel(Connection db){
        this.db=db;setLayout(new BorderLayout(14,14));setBorder(new EmptyBorder(18,22,20,22));setBackground(BusTicketApp.PALE);
        JPanel search=BusTicketApp.card();search.setLayout(new GridBagLayout());
        GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(4,7,4,7);g.fill=GridBagConstraints.HORIZONTAL;
        date.setEditor(new JSpinner.DateEditor(date,"dd/MM/yyyy"));
        addField(search,g,0,"Origin City",origin);addField(search,g,1,"Destination City",destination);addField(search,g,2,"Travel Date",date);
        JButton find=BusTicketApp.button("Search Trips");g.gridx=3;g.gridy=1;g.weightx=.5;search.add(find,g);find.addActionListener(e->searchTrips());
        loadCities(); add(search,BorderLayout.NORTH);
        JPanel center=new JPanel(new GridLayout(1,2,14,0));center.setOpaque(false);
        JPanel trips=BusTicketApp.card();trips.setLayout(new BorderLayout(8,8));trips.add(BusTicketApp.label("Available Trips"),BorderLayout.NORTH);
        tripList.setFont(new Font("Segoe UI",Font.PLAIN,14));tripList.setFixedCellHeight(48);tripList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trips.add(new JScrollPane(tripList),BorderLayout.CENTER);center.add(trips);
        JPanel right=new JPanel(new BorderLayout(8,8));right.setOpaque(false);
        JPanel seatCard=BusTicketApp.card();seatCard.setLayout(new BorderLayout(6,8));
        JPanel seatHeader=new JPanel(new BorderLayout());seatHeader.setOpaque(false);seatHeader.add(BusTicketApp.label("Seat Map"),BorderLayout.LINE_START);
        seatHeader.add(new JLabel("Available  |  Selected  |  Booked"),BorderLayout.LINE_END);seatCard.add(seatHeader,BorderLayout.NORTH);
        seatsPanel.setBackground(Color.WHITE);seatsPanel.setBorder(new EmptyBorder(10,10,10,10));seatCard.add(new JScrollPane(seatsPanel),BorderLayout.CENTER);right.add(seatCard,BorderLayout.CENTER);
        JPanel form=BusTicketApp.card();form.setLayout(new GridLayout(0,2,8,7));
        form.add(BusTicketApp.label("Passenger Name"));form.add(passenger);form.add(BusTicketApp.label("Phone Number"));form.add(phone);form.add(BusTicketApp.label("Email (optional)"));form.add(email);
        form.add(seatLabel);form.add(fareLabel);JButton confirm=BusTicketApp.button("Confirm Booking and Print Ticket");form.add(confirm);confirm.addActionListener(e->book());right.add(form,BorderLayout.SOUTH);
        center.add(right);add(center,BorderLayout.CENTER);
        tripList.addListSelectionListener(e->{if(!e.getValueIsAdjusting())selectTrip(tripList.getSelectedValue());});
    }
    private void addField(JPanel panel,GridBagConstraints g,int column,String title,JComponent component){
        g.gridx=column;g.gridy=0;g.weightx=0;panel.add(BusTicketApp.label(title),g);g.gridy=1;g.weightx=1;panel.add(component,g);
    }
    private void loadCities(){try(PreparedStatement p=db.prepareStatement("SELECT city_id,city_name FROM cities WHERE active='Y' ORDER BY city_name");ResultSet r=p.executeQuery()){
        while(r.next()){CityOption c=new CityOption(r.getLong(1),r.getString(2));origin.addItem(c);destination.addItem(c);}
    }catch(SQLException e){error(e);}}
    private void searchTrips(){
        CityOption a=(CityOption)origin.getSelectedItem(),b=(CityOption)destination.getSelectedItem();
        if(a==null||b==null){message("Add cities to the database from the Admin Panel first.");return;}if(a.id()==b.id()){message("Select two different cities.");return;}
        tripModel.clear();picked.clear();selectedTrip=null;seatsPanel.removeAll();updateTotals();
        LocalDate day=((java.util.Date)date.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String sql="SELECT s.schedule_id,c1.city_name,c2.city_name,b.bus_number,b.bus_type,s.departure_at,s.arrival_at,s.base_fare, " +
                "(SELECT COUNT(*) FROM bus_seats z WHERE z.bus_id=b.bus_id AND z.active='Y') total, " +
                "(SELECT COUNT(*) FROM booking_seats x WHERE x.schedule_id=s.schedule_id AND x.allocation_status='BOOKED') booked " +
                "FROM schedules s JOIN routes r ON r.route_id=s.route_id JOIN cities c1 ON c1.city_id=r.origin_city_id JOIN cities c2 ON c2.city_id=r.destination_city_id JOIN buses b ON b.bus_id=s.bus_id " +
                "WHERE r.origin_city_id=? AND r.destination_city_id=? AND TRUNC(CAST(s.departure_at AS DATE))=? AND s.status='SCHEDULED' AND s.departure_at>CURRENT_TIMESTAMP ORDER BY s.departure_at";
        try(PreparedStatement p=db.prepareStatement(sql)){p.setLong(1,a.id());p.setLong(2,b.id());p.setDate(3,java.sql.Date.valueOf(day));try(ResultSet r=p.executeQuery()){
            while(r.next())tripModel.addElement(new TripOption(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getTimestamp(6),r.getTimestamp(7),r.getBigDecimal(8),r.getInt(9),r.getInt(9)-r.getInt(10)));
        }}catch(SQLException e){error(e);}if(tripModel.isEmpty())message("No trips are available on the selected date.");
    }
    private void selectTrip(TripOption trip){
        selectedTrip=trip;picked.clear();seatsPanel.removeAll();updateTotals();if(trip==null)return;
        String sql="SELECT z.seat_id,z.seat_number,z.row_number,z.column_number,CASE WHEN x.seat_id IS NULL THEN 'Y' ELSE 'N' END available " +
                "FROM bus_seats z JOIN schedules s ON s.schedule_id=? LEFT JOIN booking_seats x ON x.seat_id=z.seat_id AND x.schedule_id=s.schedule_id AND x.allocation_status='BOOKED' " +
                "WHERE z.bus_id=s.bus_id AND z.active='Y' ORDER BY z.row_number,z.column_number";
        try(PreparedStatement p=db.prepareStatement(sql)){p.setLong(1,trip.id());try(ResultSet r=p.executeQuery()){
            int lastRow=-1;GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(5,7,5,7);g.fill=GridBagConstraints.BOTH;
            while(r.next()){
                long id=r.getLong(1);String name=r.getString(2);int row=r.getInt(3),col=r.getInt(4);boolean available="Y".equals(r.getString(5));
                if(row!=lastRow){JLabel label=new JLabel("Row "+row);g.gridx=0;g.gridy=row-1;seatsPanel.add(label,g);lastRow=row;}
                SeatButton button=new SeatButton(id,name,available);g.gridx=col<=2?col:col+1;g.gridy=row-1;seatsPanel.add(button,g);
            }
        }}catch(SQLException e){error(e);}seatsPanel.revalidate();seatsPanel.repaint();
    }
    private class SeatButton extends JButton{
        final long id;final String name;final boolean available;
        SeatButton(long id,String name,boolean available){super(name);this.id=id;this.name=name;this.available=available;setPreferredSize(new Dimension(62,42));setFocusPainted(false);setEnabled(available);setBackground(available?new Color(224,242,235):new Color(225,229,231));setToolTipText(available?"Available seat":"Booked");addActionListener(e->toggle());}
        void toggle(){if(picked.containsKey(id)){picked.remove(id);setBackground(new Color(224,242,235));}else if(picked.size()<8){picked.put(id,this);setBackground(new Color(244,197,91));}else{message("You can book up to 8 seats per booking.");}updateTotals();}
    }
    private void updateTotals(){seatLabel.setText("Seats: "+picked.values().stream().map(s->s.name).toList());int count=picked.size();fareLabel.setText("Total: "+(selectedTrip==null?"0":selectedTrip.fare().multiply(BigDecimal.valueOf(count)).toPlainString())+" SDG");}
    private void book(){
        if(selectedTrip==null||picked.isEmpty()){message("Select a trip and at least one seat.");return;}
        if(passenger.getText().isBlank()||phone.getText().isBlank()){message("Enter the passenger name and phone number.");return;}
        String ref="BT-"+UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase(Locale.ROOT);
        try{
            db.setAutoCommit(false);
            long busId;
            try(PreparedStatement p=db.prepareStatement("SELECT bus_id FROM schedules WHERE schedule_id=? AND status='SCHEDULED' AND departure_at>CURRENT_TIMESTAMP FOR UPDATE")){p.setLong(1,selectedTrip.id());try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("The trip has departed or was cancelled.");busId=r.getLong(1);}}
            for(long seatId:picked.keySet())try(PreparedStatement p=db.prepareStatement("SELECT COUNT(*) FROM bus_seats WHERE seat_id=? AND bus_id=? AND active='Y'")){p.setLong(1,seatId);p.setLong(2,busId);try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)!=1)throw new SQLException("A selected seat does not belong to this bus.");}}
            BigDecimal total=selectedTrip.fare().multiply(BigDecimal.valueOf(picked.size()));
            try(PreparedStatement p=db.prepareStatement("INSERT INTO bookings(booking_reference,schedule_id,passenger_name,passenger_phone,passenger_email,total_amount) VALUES(?,?,?,?,?,?)")){p.setString(1,ref);p.setLong(2,selectedTrip.id());p.setString(3,passenger.getText().trim());p.setString(4,phone.getText().trim());p.setString(5,email.getText().isBlank()?null:email.getText().trim());p.setBigDecimal(6,total);p.executeUpdate();}
            long bookingId;try(PreparedStatement p=db.prepareStatement("SELECT booking_id FROM bookings WHERE booking_reference=?")){p.setString(1,ref);try(ResultSet r=p.executeQuery()){r.next();bookingId=r.getLong(1);}}
            try(PreparedStatement p=db.prepareStatement("INSERT INTO booking_seats(booking_id,schedule_id,seat_id,fare) VALUES(?,?,?,?)")){for(long seatId:picked.keySet()){p.setLong(1,bookingId);p.setLong(2,selectedTrip.id());p.setLong(3,seatId);p.setBigDecimal(4,selectedTrip.fare());p.addBatch();}p.executeBatch();}
            db.commit();JOptionPane.showMessageDialog(this,"Booking confirmed successfully.\nBooking Reference: "+ref+"\nTrip: "+selectedTrip.origin()+" ← "+selectedTrip.destination()+"\nSeats: "+picked.values().stream().map(s->s.name).toList()+"\nTotal: "+total+" SDG","Your Ticket",JOptionPane.INFORMATION_MESSAGE);
            selectTrip(selectedTrip);searchTrips();
        }catch(SQLException e){try{db.rollback();}catch(SQLException ignored){}if(e.getErrorCode()==1)message("Another passenger just selected one of these seats. Refresh the trip and choose an available seat.");else error(e);
        }finally{try{db.setAutoCommit(false);}catch(SQLException ignored){}}
    }
    private void message(String text){JOptionPane.showMessageDialog(this,text,"Bus Ticket Manager",JOptionPane.INFORMATION_MESSAGE);}
    private void error(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}
}
