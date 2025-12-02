import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


public class UrbanServices extends JFrame {

    // Main Data
    private static List<Customer> customers = new ArrayList<>();
    private static List<Provider> providers = new ArrayList<>();
    private static List<Booking> bookings = new ArrayList<>();
    private static List<SpecialRequest> specialRequests = new ArrayList<>();
    private static User currentUser;

    //GUI COMPONENTS
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    public UrbanServices() {
        setTitle("UrbanServices - Java OOP Project");
        setSize(1100, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        DataManager.loadData();
        initUI();
    }

    private void initUI() {
        mainPanel.add(createLoginPanel(), "LOGIN");
        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
    }
    //GUI PANELS
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        JLabel title = new JLabel("UrbanServices Login");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        JTextField emailField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> {
            String email = emailField.getText();
            String pass = new String(passField.getPassword());
            if (authenticate(email, pass)) {
                if (currentUser instanceof Customer) {
                    mainPanel.add(createCustomerDashboard(), "CUSTOMER_DASH");
                    refreshCustomerView();
                    cardLayout.show(mainPanel, "CUSTOMER_DASH");
                } else {
                    mainPanel.add(createProviderDashboard(), "PROVIDER_DASH"); 
                    cardLayout.show(mainPanel, "PROVIDER_DASH");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials");
            }
        });

        JLabel hint = new JLabel("<html>Hints:<br>alice@mail.com / pass<br>bob@mail.com / pass</html>");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(title, gbc);
        gbc.gridy = 1; panel.add(new JLabel("Email:"), gbc);
        gbc.gridy = 2; panel.add(emailField, gbc);
        gbc.gridy = 3; panel.add(new JLabel("Password:"), gbc);
        gbc.gridy = 4; panel.add(passField, gbc);
        gbc.gridy = 5; panel.add(loginBtn, gbc);
        gbc.gridy = 6; panel.add(hint, gbc);

        return panel;
    }

    //CUSTOMER DASHBOARD
    private JTable providerTable;
    private DefaultTableModel providerModel;
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private JTable specialReqTable;
    private DefaultTableModel specialReqModel;

    private JPanel createCustomerDashboard() {
        if (!(currentUser instanceof Customer)) return new JPanel();

        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();

        //Tab 1:Book Service
        JPanel bookPanel = new JPanel(new BorderLayout());
        JPanel topFilter = new JPanel();
        String[] services = {"Cleaning", "Plumbing", "Electrician", "Salon"};
        JComboBox<String> serviceCombo = new JComboBox<>(services);
        JButton searchBtn = new JButton("Find Providers");
        
        providerModel = new DefaultTableModel(new String[]{"ID", "Name", "Rating", "Price/Hr"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        providerTable = new JTable(providerModel);

        providerTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = providerTable.getSelectedRow();
                    if (row != -1) {
                        String providerId = (String) providerModel.getValueAt(row, 0);
                        openProviderPortfolio(providerId);
                    }
                }
            }
        });

        searchBtn.addActionListener(e -> updateProviderTable((String) serviceCombo.getSelectedItem()));

        topFilter.add(new JLabel("Service Type:"));
        topFilter.add(serviceCombo);
        topFilter.add(searchBtn);

        JLabel infoLabel = new JLabel("Double-click a provider to view Portfolio & Book", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        infoLabel.setForeground(Color.GRAY);

        bookPanel.add(topFilter, BorderLayout.NORTH);
        bookPanel.add(new JScrollPane(providerTable), BorderLayout.CENTER);
        bookPanel.add(infoLabel, BorderLayout.SOUTH);

        //Tab 2:My Bookings
        historyModel = new DefaultTableModel(new String[]{"ID", "Service", "Provider", "Status", "Date/Time", "Price"}, 0);
        historyTable = new JTable(historyModel);
        
        JButton rateBtn = new JButton("Rate Completed Service");
        rateBtn.addActionListener(e -> rateService());

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        historyPanel.add(rateBtn, BorderLayout.SOUTH);

        //Tab 3:Request Special Services
        JPanel specialPanel = new JPanel(new BorderLayout());
        specialReqModel = new DefaultTableModel(new String[]{"Req ID", "Service Name", "Status", "Quotes"}, 0);
        specialReqTable = new JTable(specialReqModel);

        JButton postReqBtn = new JButton("Post New Request");
        postReqBtn.setBackground(new Color(70, 130, 180));
        postReqBtn.setForeground(Color.WHITE);
        postReqBtn.addActionListener(e -> showPostRequestDialog());

        JButton viewQuotesBtn = new JButton("View Quotations");
        viewQuotesBtn.addActionListener(e -> viewQuotationsForSelected());

        JPanel specialControls = new JPanel();
        specialControls.add(postReqBtn);
        specialControls.add(viewQuotesBtn);

        specialPanel.add(new JLabel("  My Special Requests (Post jobs not listed in main categories)"), BorderLayout.NORTH);
        specialPanel.add(new JScrollPane(specialReqTable), BorderLayout.CENTER);
        specialPanel.add(specialControls, BorderLayout.SOUTH);

        tabs.add("Find Services", bookPanel);
        tabs.add("My Bookings", historyPanel);
        tabs.add("Special Requests", specialPanel);

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        header.add(new JLabel("Customer Dashboard - " + currentUser.address), BorderLayout.WEST);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        header.add(logout, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    //PROVIDER DASHBOARD
    private JTable requestTable;
    private DefaultTableModel requestModel;
    private JTable marketplaceTable;
    private DefaultTableModel marketplaceModel;
    private JPanel createProviderDashboard() {
        if(!(currentUser instanceof Provider)) return new JPanel();
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        //TAB 1: JOBS
        JPanel jobsPanel = new JPanel(new BorderLayout());
        requestModel = new DefaultTableModel(new String[]{"ID", "Customer", "Details", "Status", "Earnings"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        requestTable = new JTable(requestModel);

        requestTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    showBookingDetailsProvider();
                }
            }
        });

        JPanel controls = new JPanel();
        JButton acceptBtn = new JButton("Accept Job");
        JButton startBtn = new JButton("Start Job");

        acceptBtn.addActionListener(e -> updateBookingStatus(Booking.Status.ACCEPTED));
        startBtn.addActionListener(e -> startJobProcess());

        controls.add(acceptBtn);
        controls.add(startBtn);

        jobsPanel.add(new JLabel("Double-click row for details"), BorderLayout.NORTH);
        jobsPanel.add(new JScrollPane(requestTable), BorderLayout.CENTER);
        jobsPanel.add(controls, BorderLayout.SOUTH);

        //TAB 2:MARKETPLACE
        JPanel marketPanel = new JPanel(new BorderLayout());
        marketplaceModel = new DefaultTableModel(new String[]{"Req ID", "Customer", "Service Needed", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        marketplaceTable = new JTable(marketplaceModel);
        
        JButton quoteBtn = new JButton("Send Quotation");
        quoteBtn.setBackground(new Color(255, 140, 0));
        quoteBtn.setForeground(Color.WHITE);
        quoteBtn.addActionListener(e -> sendQuotation());

        marketPanel.add(new JLabel("  Open Market - Custom Requests from Customers"), BorderLayout.NORTH);
        marketPanel.add(new JScrollPane(marketplaceTable), BorderLayout.CENTER);
        marketPanel.add(quoteBtn, BorderLayout.SOUTH);

        //TAB 3:ANALYTICS
        JPanel statsPanel = createAnalyticsPanel((Provider) currentUser);

        tabs.add("Job Requests", jobsPanel);
        tabs.add("Marketplace", marketPanel);
        tabs.add("Analytics & Stats", statsPanel);

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        header.add(new JLabel("Provider Portal - " + currentUser.name + " (" + currentUser.address + ")"), BorderLayout.WEST);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        header.add(logout, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);

        refreshProviderView();

        return panel;
    }

    private JPanel createAnalyticsPanel(Provider p) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel metricsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        metricsPanel.setPreferredSize(new Dimension(800, 100));

        long completedJobs = bookings.stream().filter(b -> b.provider.id.equals(p.id) && b.status == Booking.Status.COMPLETED).count();
        
        double actualEarnings = bookings.stream()
            .filter(b -> b.provider.id.equals(p.id) && b.status == Booking.Status.COMPLETED)
            .mapToDouble(b -> b.price)
            .sum();
        
        metricsPanel.add(createMetricCard("Total Earnings", "$" + String.format("%.2f", actualEarnings), new Color(220, 255, 220)));
        metricsPanel.add(createMetricCard("Jobs Completed", String.valueOf(completedJobs), new Color(220, 240, 255)));
        metricsPanel.add(createMetricCard("Current Rating", String.format("%.1f ⭐", p.rating), new Color(255, 250, 220)));

        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        chartsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        Map<String, Double> earningsData = new LinkedHashMap<>();
        earningsData.put("Month 1", p.hourlyRate * 10);
        earningsData.put("Month 2", p.hourlyRate * 15);
        earningsData.put("Current", actualEarnings > 0 ? actualEarnings : 0);
        chartsPanel.add(new SimpleBarChart("Earnings (Past 3 Months)", earningsData, new Color(100, 180, 100)));

        Map<String, Double> ratingData = new LinkedHashMap<>();
        ratingData.put("5 Star", 80.0);
        ratingData.put("4 Star", 15.0);
        ratingData.put("1-3 Star", 5.0);
        chartsPanel.add(new SimpleBarChart("Rating Distribution (%)", ratingData, new Color(100, 149, 237)));

        panel.add(metricsPanel, BorderLayout.NORTH);
        panel.add(chartsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMetricCard(String title, String value, Color bg) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bg);
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("Arial", Font.PLAIN, 14));
        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(new Font("Arial", Font.BOLD, 24));
        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        return card;
    }

    //SPECIAL REQUEST


    private void showPostRequestDialog() {
        JDialog dialog = new JDialog(this, "Post Special Request", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new GridBagLayout());
        dialog.setLocationRelativeTo(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField serviceNameField = new JTextField(20);
        JTextArea descArea = new JTextArea(5, 20);
        descArea.setLineWrap(true);
        JButton submitBtn = new JButton("Post Request");

        submitBtn.addActionListener(e -> {
            String name = serviceNameField.getText();
            String desc = descArea.getText();
            if(!name.isEmpty() && !desc.isEmpty()) {
                SpecialRequest req = new SpecialRequest(
                    UUID.randomUUID().toString().substring(0,8),
                    (Customer) currentUser,
                    name,
                    desc
                );
                specialRequests.add(req);
                refreshCustomerView();
                DataManager.saveData(); 
                JOptionPane.showMessageDialog(dialog, "Request Posted! Providers will see it in the Marketplace.");
                dialog.dispose();
            }
        });

        gbc.gridx=0; gbc.gridy=0; dialog.add(new JLabel("Service Name:"), gbc);
        gbc.gridx=0; gbc.gridy=1; dialog.add(serviceNameField, gbc);
        gbc.gridx=0; gbc.gridy=2; dialog.add(new JLabel("Description of work:"), gbc);
        gbc.gridx=0; gbc.gridy=3; dialog.add(new JScrollPane(descArea), gbc);
        gbc.gridx=0; gbc.gridy=4; 
        gbc.fill = GridBagConstraints.NONE; 
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(submitBtn, gbc);
        dialog.setVisible(true);
    }

    private void sendQuotation() {
        int row = marketplaceTable.getSelectedRow();
        if(row == -1) {
            JOptionPane.showMessageDialog(this, "Select a request to quote.");
            return;
        }
        String reqId = (String) marketplaceModel.getValueAt(row, 0);
        SpecialRequest req = specialRequests.stream().filter(r -> r.id.equals(reqId)).findFirst().orElse(null);

        if(req != null) {
            JDialog dialog = new JDialog(this, "Submit Quotation", true);
            dialog.setSize(400, 300);
            dialog.setLayout(new GridLayout(5, 1));
            dialog.setLocationRelativeTo(this);

            JTextField priceField = new JTextField();
            JTextArea pitchArea = new JTextArea();
            pitchArea.setBorder(BorderFactory.createTitledBorder("Why are you suitable?"));
            JButton submitBtn = new JButton("Send Quote");

            submitBtn.addActionListener(e -> {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    Quotation q = new Quotation((Provider)currentUser, price, pitchArea.getText());
                    req.quotations.add(q);
                    DataManager.saveData(); 
                    JOptionPane.showMessageDialog(dialog, "Quotation Sent!");
                    dialog.dispose();
                } catch(NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Invalid Price");
                }
            });

            dialog.add(new JLabel("Your Price ($):"));
            dialog.add(priceField);
            dialog.add(new JScrollPane(pitchArea));
            dialog.add(submitBtn);
            dialog.setVisible(true);
        }
    }

    private void viewQuotationsForSelected() {
        int row = specialReqTable.getSelectedRow();
        if(row == -1) return;
        
        String reqId = (String) specialReqModel.getValueAt(row, 0);
        SpecialRequest req = specialRequests.stream().filter(r -> r.id.equals(reqId)).findFirst().orElse(null);

        if(req == null || req.quotations.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No quotations received yet.");
            return;
        }

        JDialog dialog = new JDialog(this, "Received Quotations", true);
        dialog.setSize(500, 400);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        Map<Integer, Quotation> indexMap = new HashMap<>();
        
        int i = 0;
        for(Quotation q : req.quotations) {
            String summary = q.provider.name + " | $" + q.price + " | " + q.pitch;
            listModel.addElement(summary);
            indexMap.put(i++, q);
        }

        JList<String> qList = new JList<>(listModel);
        JButton acceptBtn = new JButton("Accept Selected Quote");

        acceptBtn.addActionListener(e -> {
            int idx = qList.getSelectedIndex();
            if(idx != -1) {
                Quotation selectedQ = indexMap.get(idx);
                String date = JOptionPane.showInputDialog("Enter Date (YYYY-MM-DD):");
                String time = JOptionPane.showInputDialog("Enter Time Block:");
                
                if(date != null && time != null) {
                    createBooking(selectedQ.provider, date, time, 
                        "Special Req: " + req.serviceName + " - " + req.description, selectedQ.price);
                    
                    req.status = "CLOSED";
                    refreshCustomerView();
                    dialog.dispose();
                }
            }
        });

        dialog.add(new JLabel("Select a quote to accept:"), BorderLayout.NORTH);
        dialog.add(new JScrollPane(qList), BorderLayout.CENTER);
        dialog.add(acceptBtn, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    //           SHARED & HELPER LOGIC

    private void openProviderPortfolio(String providerId) {
        Provider p = providers.stream().filter(pr -> pr.id.equals(providerId)).findFirst().orElse(null);
        if (p == null) return;

        JDialog dialog = new JDialog(this, "Provider Portfolio", true);
        dialog.setSize(500, 600);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        infoPanel.setBackground(new Color(230, 240, 250));

        JLabel nameLbl = new JLabel(p.name);
        nameLbl.setFont(new Font("Arial", Font.BOLD, 22));
        JLabel catLbl = new JLabel("Expert " + p.serviceCategory);
        catLbl.setFont(new Font("Arial", Font.ITALIC, 16));
        JLabel rateLbl = new JLabel("Rating: " + String.format("%.1f", p.rating) + " ⭐ (" + p.ratingCount + " reviews)");
        JLabel priceLbl = new JLabel("Price: $" + p.hourlyRate + "/hr");

        infoPanel.add(nameLbl);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(catLbl);
        infoPanel.add(rateLbl);
        infoPanel.add(priceLbl);

        DefaultListModel<String> reviewListModel = new DefaultListModel<>();
        for (String r : p.reviews) reviewListModel.addElement(r);
        JList<String> reviewList = new JList<>(reviewListModel);
        JScrollPane scrollReviews = new JScrollPane(reviewList);
        scrollReviews.setBorder(BorderFactory.createTitledBorder("Customer Reviews"));

        JButton bookBtn = new JButton("Book A Service");
        bookBtn.setFont(new Font("Arial", Font.BOLD, 16));
        bookBtn.setBackground(new Color(60, 179, 113));
        bookBtn.setForeground(Color.WHITE);
        bookBtn.addActionListener(e -> {
            dialog.dispose();
            showBookingDialog(p);
        });

        dialog.add(infoPanel, BorderLayout.NORTH);
        dialog.add(scrollReviews, BorderLayout.CENTER);
        dialog.add(bookBtn, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showBookingDialog(Provider p) {
        JDialog dialog = new JDialog(this, "Book Service with " + p.name, true);
        dialog.setSize(400, 400);
        dialog.setLayout(new GridBagLayout());
        dialog.setLocationRelativeTo(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField dateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        String[] times = {"09:00 AM - 11:00 AM", "11:00 AM - 01:00 PM", "02:00 PM - 04:00 PM", "04:00 PM - 06:00 PM"};
        JComboBox<String> timeCombo = new JComboBox<>(times);
        JTextArea commentArea = new JTextArea(5, 20);
        commentArea.setLineWrap(true);
        JScrollPane commentScroll = new JScrollPane(commentArea);

        double estimatedPrice = p.hourlyRate * 2.0;

        JButton confirmBtn = new JButton("Confirm Booking ($" + estimatedPrice + ")");
        confirmBtn.addActionListener(e -> {
            createBooking(p, dateField.getText(), (String) timeCombo.getSelectedItem(), commentArea.getText(), estimatedPrice);
            dialog.dispose();
        });

        gbc.gridx = 0; gbc.gridy = 0; dialog.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 0; gbc.gridy = 1; dialog.add(dateField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; dialog.add(new JLabel("Time Block (2 hrs):"), gbc);
        gbc.gridx = 0; gbc.gridy = 3; dialog.add(timeCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 4; dialog.add(new JLabel("Notes for Provider:"), gbc);
        gbc.gridx = 0; gbc.gridy = 5; dialog.add(commentScroll, gbc);
        gbc.gridx = 0; gbc.gridy = 6; dialog.add(confirmBtn, gbc);

        dialog.setVisible(true);
    }

    private void createBooking(Provider p, String date, String time, String notes, double price) {
        Booking b = new Booking(
            UUID.randomUUID().toString().substring(0,8), 
            (Customer) currentUser, 
            p, 
            p.serviceCategory, 
            date, 
            time, 
            notes,
            price
        );
        bookings.add(b);
        JOptionPane.showMessageDialog(this, "Booking Request Sent! Cost: $" + price);
        refreshCustomerView();
        DataManager.saveData(); 
    }

    private void rateService() {
        int row = historyTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking to rate.");
            return;
        }

        String status = historyModel.getValueAt(row, 3).toString();
        if (!status.equals("COMPLETED")) {
            JOptionPane.showMessageDialog(this, "Service must be completed to rate.");
            return;
        }

        String bookingId = (String) historyModel.getValueAt(row, 0);
        Booking booking = bookings.stream().filter(b -> b.id.equals(bookingId)).findFirst().orElse(null);

        JDialog rateDialog = new JDialog(this, "Rate Service", true);
        rateDialog.setSize(300, 300);
        rateDialog.setLayout(new GridLayout(4, 1));
        rateDialog.setLocationRelativeTo(this);

        JSlider ratingSlider = new JSlider(1, 5, 5);
        ratingSlider.setMajorTickSpacing(1);
        ratingSlider.setPaintTicks(true);
        ratingSlider.setPaintLabels(true);

        JTextField commentField = new JTextField();
        JButton submitBtn = new JButton("Submit Review");

        submitBtn.addActionListener(e -> {
            if (booking != null) {
                booking.provider.addReview(ratingSlider.getValue(), commentField.getText());
                JOptionPane.showMessageDialog(this, "Thank you for your feedback!");
                DataManager.saveData(); 
                refreshCustomerView();
                rateDialog.dispose();
            }
        });

        rateDialog.add(new JLabel("Rating:", SwingConstants.CENTER));
        rateDialog.add(ratingSlider);
        rateDialog.add(new JLabel("Comment (Optional):", SwingConstants.CENTER));
        rateDialog.add(commentField);
        rateDialog.add(submitBtn);

        rateDialog.setVisible(true);
    }

    private void startJobProcess() {
        int row = requestTable.getSelectedRow();
        if (row == -1) return;
        String bookingId = (String) requestModel.getValueAt(row, 0);
        updateBookingStatusLogic(bookingId, Booking.Status.IN_PROGRESS);
        refreshProviderView();

        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Job Started..."));
                Thread.sleep(5000); 
                updateBookingStatusLogic(bookingId, Booking.Status.COMPLETED);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Job Completed! Earnings added.");
                    refreshProviderView();
                });
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void showBookingDetailsProvider() {
        int row = requestTable.getSelectedRow();
        if (row == -1) return;
        
        String bookingId = (String) requestModel.getValueAt(row, 0);
        Booking b = bookings.stream().filter(bk -> bk.id.equals(bookingId)).findFirst().orElse(null);
        
        if (b != null) {
            String noteText = (b.requestComments == null || b.requestComments.isEmpty()) ? "None" : b.requestComments;
            String msg = "Customer: " + b.customer.name + "\n" +
                         "Address: " + b.customer.address + "\n" + // Show Address
                         "Service: " + b.serviceType + "\n" +
                         "Earnings: $" + b.price + "\n" +
                         "Date: " + b.date + "\n" +
                         "Time: " + b.timeBlock + "\n" +
                         "Notes: " + noteText;
            JOptionPane.showMessageDialog(this, msg, "Booking Details", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private boolean authenticate(String email, String pass) {
        for (Customer c : customers) {
            if (c.email.equals(email) && c.password.equals(pass)) {
                currentUser = c;
                return true;
            }
        }
        for (Provider p : providers) {
            if (p.email.equals(email) && p.password.equals(pass)) {
                currentUser = p;
                return true;
            }
        }
        return false;
    }

    private void updateProviderTable(String category) {
        providerModel.setRowCount(0);
        List<Provider> matches = providers.stream()
                .filter(p -> p.serviceCategory.equalsIgnoreCase(category))
                .sorted(Comparator.comparingDouble(Provider::getRating).reversed())
                .collect(Collectors.toList());

        for (Provider p : matches) {
            providerModel.addRow(new Object[]{p.id, p.name, String.format("%.1f", p.rating), "$" + p.hourlyRate});
        }
    }

    private void updateBookingStatus(Booking.Status newStatus) {
        int row = requestTable.getSelectedRow();
        if (row != -1) {
            String id = (String) requestModel.getValueAt(row, 0);
            updateBookingStatusLogic(id, newStatus);
            refreshProviderView();
        } else {
            JOptionPane.showMessageDialog(this, "Select a request first.");
        }
    }

    private void updateBookingStatusLogic(String id, Booking.Status status) {
        for (Booking b : bookings) {
            if (b.id.equals(id)) {
                b.status = status;
                break;
            }
        }
        DataManager.saveData(); 
    }

    private void refreshCustomerView() {
        historyModel.setRowCount(0);
        for (Booking b : bookings) {
            if (b.customer.id.equals(currentUser.id)) {
                String dateTime = (b.date != null ? b.date : "") + " " + (b.timeBlock != null ? b.timeBlock : "");
                historyModel.addRow(new Object[]{b.id, b.serviceType, b.provider.name, b.status.toString(), dateTime, "$" + b.price});
            }
        }
        specialReqModel.setRowCount(0);
        for(SpecialRequest r : specialRequests) {
            if(r.customer.id.equals(currentUser.id)) {
                specialReqModel.addRow(new Object[]{r.id, r.serviceName, r.status, r.quotations.size() + " quotes"});
            }
        }
    }

    private void refreshProviderView() {
        if (requestModel == null) return; 
        
        requestModel.setRowCount(0);
        for (Booking b : bookings) {
            if (b.provider.id.equals(currentUser.id)) {
                String summary = b.date + " (" + b.timeBlock + ")";
                requestModel.addRow(new Object[]{b.id, b.customer.name, summary, b.status, "$" + b.price});
            }
        }

        marketplaceModel.setRowCount(0);
        for (SpecialRequest r : specialRequests) {
            if(r.status.equals("OPEN")) {
                marketplaceModel.addRow(new Object[]{r.id, r.customer.name, r.serviceName, r.description});
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UrbanServices().setVisible(true);
        });
    }

    //MODELS

    static abstract class User {
        String id, name, email, password, address; // Added address
        public User(String id, String name, String email, String password, String address) {
            this.id = id; this.name = name; this.email = email; this.password = password; this.address = address;
        }
    }

    static class Customer extends User {
        public Customer(String id, String name, String email, String password, String address) {
            super(id, name, email, password, address);
        }
    }

    static class Provider extends User {
        String serviceCategory;
        double rating;
        double hourlyRate;
        int ratingCount = 0;
        List<String> reviews = new ArrayList<>();

        public Provider(String id, String name, String email, String password, String address, String cat, double rate, double rating) {
            super(id, name, email, password, address);
            this.serviceCategory = cat;
            this.hourlyRate = rate;
            this.rating = rating;
            if(rating > 0) this.ratingCount = 1;
            reviews.add("Great service! - System Seed");
        }

        public double getRating() { return rating; }

        public void addReview(int score, String comment) {
            double totalScore = this.rating * this.ratingCount;
            totalScore += score;
            this.ratingCount++;
            this.rating = totalScore / this.ratingCount;
            if(comment != null && !comment.isEmpty()) {
                this.reviews.add(0, "★" + score + ": " + comment);
            }
        }
    }

    static class Booking {
        enum Status { PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }
        String id;
        Customer customer;
        Provider provider;
        String serviceType;
        Status status;
        String date, timeBlock, requestComments;
        double price; 

        public Booking(String id, Customer c, Provider p, String s, String date, String time, String notes, double price) {
            this.id = id; this.customer = c; this.provider = p; this.serviceType = s;
            this.status = Status.PENDING;
            this.date = date;
            this.timeBlock = time;
            this.requestComments = notes;
            this.price = price;
        }
    }

    static class SpecialRequest {
        String id;
        Customer customer;
        String serviceName;
        String description;
        String status; 
        List<Quotation> quotations = new ArrayList<>();

        public SpecialRequest(String id, Customer c, String name, String desc) {
            this.id = id; this.customer = c; this.serviceName = name; this.description = desc;
            this.status = "OPEN";
        }
    }

    static class Quotation {
        Provider provider;
        double price;
        String pitch;

        public Quotation(Provider p, double price, String pitch) {
            this.provider = p; this.price = price; this.pitch = pitch;
        }
    }

    //CUSTOM CHART COMPONENT

    static class SimpleBarChart extends JPanel {
        private String title;
        private Map<String, Double> data;
        private Color barColor;

        public SimpleBarChart(String title, Map<String, Double> data, Color barColor) {
            this.title = title;
            this.data = data;
            this.barColor = barColor;
            setBorder(BorderFactory.createTitledBorder(title));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int padding = 30;

            if(data.isEmpty()) return;

            double maxVal = data.values().stream().mapToDouble(v -> v).max().orElse(1.0);
            
            int barWidth = (width - (2 * padding)) / data.size() - 20;
            int x = padding;

            for (Map.Entry<String, Double> entry : data.entrySet()) {
                int barHeight = (int) ((entry.getValue() / maxVal) * (height - 2 * padding - 20));
                int y = height - padding - barHeight;

                g2.setColor(barColor);
                g2.fillRect(x, y, barWidth, barHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, barWidth, barHeight);

                g2.drawString(entry.getKey(), x, height - 10);
                g2.drawString(String.format("%.0f", entry.getValue()), x + 5, y - 5);

                x += barWidth + 20;
            }
        }
    }

    //              DATA MANAGER
    static class DataManager {
        private static final String FILE_NAME = "database.csv";

        public static void loadData() {
            File f = new File(FILE_NAME);
            if (!f.exists()) {
                seedData();
                saveData();
                return;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                customers.clear();
                providers.clear();
                bookings.clear();
                specialRequests.clear();

                String line;
                List<String[]> bookingData = new ArrayList<>();
                List<String[]> requestData = new ArrayList<>();
                List<String[]> quoteData = new ArrayList<>();

                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",,"); 
                    if (parts.length == 0) continue;

                    String type = parts[0];
                    switch (type) {
                        case "C":
                            // ID, Name, Email, Password, Address
                            customers.add(new Customer(parts[1], parts[2], parts[3], parts[4], parts[5]));
                            break;
                        case "P":
                            // ID, Name, Email, Password, Address, Category, Rate, Rating, Count, Reviews
                            Provider p = new Provider(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], 
                                Double.parseDouble(parts[7]), Double.parseDouble(parts[8]));
                            p.ratingCount = Integer.parseInt(parts[9]);
                            p.reviews.clear();
                            if (parts.length > 10 && !parts[10].isEmpty()) {
                                String[] revs = parts[10].split("::");
                                for (String r : revs) p.reviews.add(r);
                            }
                            providers.add(p);
                            break;
                        case "B":
                            bookingData.add(parts);
                            break;
                        case "SR":
                            requestData.add(parts);
                            break;
                        case "Q":
                            quoteData.add(parts);
                            break;
                    }
                }

                // Process Bookings
                for (String[] b : bookingData) {
                    Customer c = findCustomer(b[2]);
                    Provider p = findProvider(b[3]);
                    if (c != null && p != null) {
                        Booking booking = new Booking(b[1], c, p, b[4], b[6], b[7], b[8].replace("~~", "\n"), Double.parseDouble(b[9]));
                        booking.status = Booking.Status.valueOf(b[5]);
                        bookings.add(booking);
                    }
                }

                // Process Special Requests
                for (String[] sr : requestData) {
                    Customer c = findCustomer(sr[2]);
                    if (c != null) {
                        SpecialRequest req = new SpecialRequest(sr[1], c, sr[3], sr[4].replace("~~", "\n"));
                        req.status = sr[5];
                        specialRequests.add(req);
                    }
                }

                // Process Quotations
                for (String[] q : quoteData) {
                    SpecialRequest req = findRequest(q[1]);
                    Provider p = findProvider(q[2]);
                    if (req != null && p != null) {
                        req.quotations.add(new Quotation(p, Double.parseDouble(q[3]), q[4].replace("~~", "\n")));
                    }
                }

            } catch (Exception e) {
                seedData(); 
            }
        }

        public static void saveData() {
            try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
                // Customers
                for (Customer c : customers) {
                    pw.println("C,," + c.id + ",," + c.name + ",," + c.email + ",," + c.password + ",," + c.address);
                }
                // Providers
                for (Provider p : providers) {
                    String reviewStr = String.join("::", p.reviews);
                    pw.println("P,," + p.id + ",," + p.name + ",," + p.email + ",," + p.password + ",," + p.address + ",," +
                            p.serviceCategory + ",," + p.hourlyRate + ",," + p.rating + ",," + p.ratingCount + ",," + reviewStr);
                }
                // Bookings
                for (Booking b : bookings) {
                    String safeComments = b.requestComments != null ? b.requestComments.replace("\n", "~~") : "";
                    pw.println("B,," + b.id + ",," + b.customer.id + ",," + b.provider.id + ",," +
                            b.serviceType + ",," + b.status + ",," + b.date + ",," + b.timeBlock + ",," + safeComments + ",," + b.price);
                }
                // Special Requests
                for (SpecialRequest sr : specialRequests) {
                    String safeDesc = sr.description.replace("\n", "~~");
                    pw.println("SR,," + sr.id + ",," + sr.customer.id + ",," + sr.serviceName + ",," + safeDesc + ",," + sr.status);
                    
                    for (Quotation q : sr.quotations) {
                        String safePitch = q.pitch.replace("\n", "~~");
                        pw.println("Q,," + sr.id + ",," + q.provider.id + ",," + q.price + ",," + safePitch);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Linking Helpers
        private static Customer findCustomer(String id) {
            return customers.stream().filter(c -> c.id.equals(id)).findFirst().orElse(null);
        }
        private static Provider findProvider(String id) {
            return providers.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null);
        }
        private static SpecialRequest findRequest(String id) {
            return specialRequests.stream().filter(r -> r.id.equals(id)).findFirst().orElse(null);
        }

        private static void seedData() {
            customers.clear();
            providers.clear();
            
            customers.add(new Customer("C1", "Alice", "alice@mail.com", "pass", "123 Main St, New York"));
            customers.add(new Customer("C2", "John", "john@mail.com", "pass", "456 Elm Ave, Brooklyn"));

            // Plumbing
            providers.add(new Provider("P1", "Bob Fixer", "bob@mail.com", "pass", "789 Pine Rd, NY", "Plumbing", 50.0, 4.5));
            providers.add(new Provider("P2", "Mario Bros", "mario@mail.com", "pass", "101 Mushroom St, NY", "Plumbing", 80.0, 5.0));
            providers.add(new Provider("P3", "Leak Masters", "leak@mail.com", "pass", "202 Water Way, NY", "Plumbing", 45.0, 4.0));

            // Cleaning
            providers.add(new Provider("P4", "Sara Clean", "sara@mail.com", "pass", "303 Dust Ln, NY", "Cleaning", 30.0, 4.8));
            providers.add(new Provider("P5", "Dust Busters", "dust@mail.com", "pass", "404 Broom Blvd, NY", "Cleaning", 25.0, 4.2));
            providers.add(new Provider("P6", "Sparkle Inc", "sparkle@mail.com", "pass", "505 Shine St, NY", "Cleaning", 40.0, 4.9));

            // Electrician
            providers.add(new Provider("P7", "Mike Spark", "mike@mail.com", "pass", "606 Volt Ave, NY", "Electrician", 60.0, 4.2));
            providers.add(new Provider("P8", "Volt Kings", "volt@mail.com", "pass", "707 Ampere Dr, NY", "Electrician", 70.0, 4.7));
            providers.add(new Provider("P9", "Bright Lights", "bright@mail.com", "pass", "808 Bulb St, NY", "Electrician", 55.0, 4.4));

            // Salon
            providers.add(new Provider("P10", "Glamour Co", "glam@mail.com", "pass", "909 Style Sq, NY", "Salon", 45.0, 4.9));
            providers.add(new Provider("P11", "Style Studio", "style@mail.com", "pass", "001 Fashion Rd, NY", "Salon", 50.0, 4.6));
            providers.add(new Provider("P12", "Beauty Box", "box@mail.com", "pass", "112 Chic Cir, NY", "Salon", 35.0, 4.1));
        }
    }
}