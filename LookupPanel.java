package com.busticket.desktop;

import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

class LookupPanel extends JPanel {
    private final Connection db;private final JTextField reference=new JTextField(14),phone=new JTextField(16);
    LookupPanel(Connection db){this.db=db;setLayout(new BorderLayout());setBackground(BusTicketApp.PALE);JPanel form=BusTicketApp.card();form.setLayout(new GridBagLayout());
        GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(8,9,8,9);g.fill=GridBagConstraints.HORIZONTAL;
        g.gridx=0;g.gridy=0;form.add(BusTicketApp.label("Booking Reference"),g);g.gridx=1;form.add(reference,g);
        g.gridx=0;g.gridy=1;form.add(BusTicketApp.label("Passenger Phone"),g);g.gridx=1;form.add(phone,g);
        JButton find=BusTicketApp.button("View Ticket");g.gridx=1;g.gridy=2;form.add(find,g);find.addActionListener(e->showBooking());
        JPanel wrap=new JPanel(new GridBagLayout());wrap.setOpaque(false);wrap.setBorder(new EmptyBorder(30,30,30,30));wrap.add(form);add(wrap,BorderLayout.NORTH);
    }
    private void showBooking(){
        if(reference.getText().isBlank()||phone.getText().isBlank()){message("Enter the booking reference and phone number.");return;}
        String sql="SELECT b.booking_id,b.booking_reference,b.passenger_name,b.passenger_phone,b.total_amount,b.status,b.payment_status,s.departure_at,s.arrival_at,c1.city_name,c2.city_name,bus.bus_number " +
                "FROM bookings b JOIN schedules s ON s.schedule_id=b.schedule_id JOIN routes r ON r.route_id=s.route_id JOIN cities c1 ON c1.city_id=r.origin_city_id JOIN cities c2 ON c2.city_id=r.destination_city_id JOIN buses bus ON bus.bus_id=s.bus_id " +
                "WHERE b.booking_reference=? AND b.passenger_phone=?";
        try(PreparedStatement p=db.prepareStatement(sql)){p.setString(1,reference.getText().trim().toUpperCase());p.setString(2,phone.getText().trim());try(ResultSet r=p.executeQuery()){
            if(!r.next()){message("No booking was found with those details.");return;}
            long id=r.getLong(1);String ref=r.getString(2),name=r.getString(3),status=r.getString(6);String seats=seatNames(id);
            String details="Ticket: "+ref+"\nPassenger: "+name+"\nTrip: "+r.getString(10)+" ← "+r.getString(11)+"\nBus: "+r.getString(12)+"\nDeparture: "+r.getTimestamp(8)+"\nArrival: "+r.getTimestamp(9)+"\nSeats: "+seats+"\nTotal: "+r.getBigDecimal(5)+" SDG\nStatus: "+status+" / "+r.getString(7);
            if("CONFIRMED".equals(status)){
                int choice=JOptionPane.showConfirmDialog(this,details+"\n\nWould you like to cancel this booking?", "Ticket Details",JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE);
                if(choice==JOptionPane.YES_OPTION)cancel(id,ref);
            }else JOptionPane.showMessageDialog(this,details,"Ticket Details",JOptionPane.INFORMATION_MESSAGE);
        }}catch(SQLException e){message(e.getMessage());}
    }
    private String seatNames(long id)throws SQLException{try(PreparedStatement p=db.prepareStatement("SELECT LISTAGG(z.seat_number,', ') WITHIN GROUP(ORDER BY z.seat_number) FROM booking_seats x JOIN bus_seats z ON z.seat_id=x.seat_id WHERE x.booking_id=?")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){r.next();return r.getString(1);}}}
    private void cancel(long id,String ref){
        try{db.setAutoCommit(false);try(PreparedStatement p=db.prepareStatement("UPDATE bookings SET status='CANCELLED',payment_status=CASE WHEN payment_status='PAID' THEN 'REFUND_PENDING' ELSE payment_status END,cancelled_at=CURRENT_TIMESTAMP WHERE booking_id=? AND status='CONFIRMED' AND schedule_id IN (SELECT schedule_id FROM schedules WHERE departure_at>CURRENT_TIMESTAMP)")){p.setLong(1,id);int updated=p.executeUpdate();if(updated==0){db.rollback();message("This booking cannot be cancelled; the trip may have already departed.");return;}}
            try(PreparedStatement p=db.prepareStatement("UPDATE booking_seats SET allocation_status='CANCELLED' WHERE booking_id=?")){p.setLong(1,id);p.executeUpdate();}db.commit();message("Booking cancelled: "+ref+". The seats are available again.");
        }catch(SQLException e){try{db.rollback();}catch(SQLException ignored){}message(e.getMessage());}finally{try{db.setAutoCommit(false);}catch(SQLException ignored){}}
    }
    private void message(String m){JOptionPane.showMessageDialog(this,m,"Booking Management",JOptionPane.INFORMATION_MESSAGE);}
}
