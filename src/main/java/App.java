import com.fasterxml.uuid.Generators;
import com.google.gson.Gson;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static spark.Spark.*;

public class App {
    public static ArrayList<Car> cars = new ArrayList<>();

    public static HashMap<String, BaseColor> colorsMap = new HashMap<>() {{
        put("red", BaseColor.RED);
        put("blue", BaseColor.BLUE);
        put("black", BaseColor.BLACK);
        put("green", BaseColor.GREEN);
    }};

    public static ArrayList<String> colors = new ArrayList<>() {
        {
            add("red");
            add("blue");
            add("black");
            add("green");
        }
    };
    public static ArrayList<String> models = new ArrayList<>() {
        {
            add("Toyota");
            add("Skoda");
            add("Mercedes");
            add("Audi");
            add("VW");
        }
    };

    public static ArrayList<Integer> vatList = new ArrayList<>(){
        {
            add(0);
            add(7);
            add(22);
        }
    };


    public static HashMap<String, String> modelsMap = new HashMap<>() {{
        put("Toyota", "src/main/resources/public/imgs/toyota.png");
        put("Skoda", "src/main/resources/public/imgs/skoda.png");
        put("Mercedes", "src/main/resources/public/imgs/mercedes.png");
        put("Audi", "src/main/resources/public/imgs/audi.png");
        put("VW", "src/main/resources/public/imgs/vw.png");
    }};

    public static void main(String[] args) {
//        externalStaticFileLocation("C:\\Users\\4pa\\Desktop\\java\\sparkProject\\src\\main\\resources\\public");
        externalStaticFileLocation("C:\\Users\\pauli\\sparkProject\\src\\main\\resources\\public");
        post("/add", App::add);
        get("/json", App::getJson);
        post("/delete", App::delete);
        post("/update", App::update);
        post("/generate", App::generate);
        post("/invoice", App::invoice);
        get("/getInvoice", App::getInvoice);
        post("/manyCarsInvoice", App::manyCarsInvoice);
        post("/upload", App::upload);
        get("/thumb", App::getThumb);
    }

    static String add(Request req, Response res){
        System.out.println(req.body());
        Gson gson = new Gson();
        Car car = gson.fromJson(req.body(), Car.class);
        cars.add(car);
        System.out.println("car " + car.toString());
        res.type("application/json");
        return gson.toJson(car);
    }

    static String delete(Request req, Response res){
        System.out.println(req.body());
        Gson gson = new Gson();
        int id = gson.fromJson(req.body(), IdToDelete.class).getId();
        System.out.println(id);

        cars.removeIf(c -> c.id == id);
        res.type("application/json");
        return gson.toJson(cars);
    }

    static String update(Request req, Response res){
        System.out.println(req.body());
        Gson gson = new Gson();
        UpdatedData data = gson.fromJson(req.body(), UpdatedData.class);
        System.out.println(data);
        for(Car c: cars){
            if(c.getId() == data.getId()){
                c.setModel(data.getModel());
                c.setYear(data.getYear());
                break;
            }
        }
        res.type("application/json");
        return gson.toJson(cars);
    }

    static String getJson(Request req, Response res){
        res.type("application/json");
        Gson gson = new Gson();
        return gson.toJson(cars);
    }

    static String generate(Request req, Response res){
        for(int i=0; i<10; i++){
            Collections.shuffle(models);
            Collections.shuffle(colors);
            int year = (int) ((Math.random() * 10) + 2000);

            Random rd = new Random();
            ArrayList<Airbag> airbags = new ArrayList<>();
            airbags.add(new Airbag("kierowca", rd.nextBoolean()));
            airbags.add(new Airbag("pasażer", rd.nextBoolean()));
            airbags.add(new Airbag("kanapa", rd.nextBoolean()));
            airbags.add(new Airbag("boczne", rd.nextBoolean()));

            Car randomCar = new Car(models.get(0), year, colors.get(0), airbags);
            cars.add(randomCar);
        }
        return "ok";
    }

    static String invoice(Request req, Response res) throws IOException, DocumentException {
        Gson gson = new Gson();
        int id = gson.fromJson(req.body(), IdToDelete.class).getId();
        System.out.println(id);

        Car car = new Car();
        for(Car c: cars){
            if(c.getId() == id){
                car = c;
                c.setInvoice(true);
                break;
            }
        }

        Document document = new Document(); // dokument pdf
        String path = "src/main/resources/public/pdfs/invoice" + car.getUuid() + ".pdf"; // lokalizacja zapisu
        PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();

        Font font = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);
        Font colorFont = FontFactory.getFont(FontFactory.COURIER, 16, colorsMap.get(car.getColor()));
        Chunk chunk = new Chunk("Faktura dla: " + car.getUuid(), font); // akapit
        document.add(chunk);

        Paragraph p = new Paragraph("model: " + car.getModel(), font);
        document.add(p);

        Paragraph p2 = new Paragraph("kolor: " + car.getColor(), colorFont);
        document.add(p2);

        Paragraph p3 = new Paragraph("rok: " + car.getYear(), font);
        document.add(p3);

        for(int i=0; i<4; i++){
            Paragraph p4 = new Paragraph("poduszka: " + car.getAirbags().get(i).desc + " - " + car.getAirbags().get(i).value, font);
            document.add(p4);
        }

        Image img;
        if(models.contains(car.getModel())){
            img = Image.getInstance(modelsMap.get(car.getModel()));
        }else{
            img = Image.getInstance("src/main/resources/public/imgs/car.png");
        }
        document.add(img);

        document.close();

        return "ok";
    }

    static String getInvoice(Request req, Response res) throws IOException {
        String fileName = "invoice" + req.queryParams("uuid") + ".pdf";
        res.type("application/octet-stream"); //
        res.header("Content-Disposition", "attachment; filename=" + fileName); // nagłówek

        OutputStream outputStream = res.raw().getOutputStream();
        outputStream.write(Files.readAllBytes(Path.of("src/main/resources/public/pdfs/" + fileName))); // response pliku do przeglądarki
        return null;
    }

    static String manyCarsInvoice(Request req, Response res) throws DocumentException, FileNotFoundException {
        Gson gson = new Gson();
        int year = gson.fromJson(req.body(), InvoiceData.class).getYear();
        int low = gson.fromJson(req.body(), InvoiceData.class).getLow();
        int high = gson.fromJson(req.body(), InvoiceData.class).getHigh();
        System.out.println(year + " " + low + " " + high);

        if (year == 0 && low == 0 && high == 0){
            Invoice inv = new Invoice("sprzedawca", "kupujący", cars);
            inv.generatePdf("Faktura za wszystkie auta");
        }else if(year != 0 && low == 0 && high == 0){
            Stream<Car> result = cars.stream().filter(s->s.year == year);

            Invoice inv = new Invoice("sprzedawca", "kupujący", result.collect(Collectors.toCollection(ArrayList::new)));
            inv.generatePdf("Faktura za auta z roku " + year);
        }else{
            Stream<Car> result = cars.stream().filter(s->s.price <= high);
            Stream<Car> result2 = result.filter(s->s.price >= low);

            Invoice inv = new Invoice("sprzedawca", "kupujący", result2.collect(Collectors.toCollection(ArrayList::new)));
            inv.generatePdf("Faktura za auta w cenach " + low + "-" + high + " PLN");
        }

        return null;
    }

    static String upload(Request req, Response res) throws ServletException, IOException {
        req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/images"));
        System.out.println("plików jest: "+req.raw().getParts().size());
        System.out.println("parts "+req.raw().getParts());

        ArrayList<String> fileNames = new ArrayList<>();

        for(Part p : req.raw().getParts()){
            System.out.println(p.getSubmittedFileName());
            System.out.println(p.getInputStream());
            InputStream inputStream = p.getInputStream();
            // inputstream to byte
            byte[] bytes = inputStream.readAllBytes();
            String fileName = p.getSubmittedFileName();
            FileOutputStream fos = new FileOutputStream("images/" + fileName);
            fos.write(bytes);
            fos.close();
            // dodaj do Arraylist z nazwami aut do odesłania do przeglądarki
            fileNames.add(fileName);
        }
        String json = new Gson().toJson(fileNames);
        return json;
    }

    static String getThumb(Request req, Response res) throws IOException {
        String fileName = req.queryParams("id");
        res.type("image/jpeg");

        OutputStream outputStream = res.raw().getOutputStream();

        outputStream.write(Files.readAllBytes(Path.of("images/" + fileName)));
        outputStream.flush();

        return outputStream.toString();
    }

}

class Airbag {
    String desc;
    boolean value;

    public Airbag(String name, boolean value) {
        this.desc = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Airbag{" +
                "desc='" + desc + '\'' +
                ", value=" + value +
                '}';
    }
}

class Car{
    int id;
    UUID uuid;
    String model;
    int year;
    String color;
    ArrayList<Airbag> airbags;
    boolean invoice;
    int price;
    int vat;
    CustomDate date;
    String img;

    public Car() {
        if (App.cars.isEmpty()) {
            this.id = 1;
        }else{
            this.id = App.cars.get(App.cars.size()-1).id + 1;
        }
        this.uuid = Generators.randomBasedGenerator().generate();
        this.invoice = false;
    }

    public Car( String model, int year, String color, ArrayList<Airbag> airbags) {
        if (App.cars.isEmpty()) {
            this.id = 1;
        }else{
            this.id = App.cars.get(App.cars.size()-1).id + 1;
        }
        this.uuid = Generators.randomBasedGenerator().generate();
        this.model = model;
        this.year = year;
        this.color = color;
        this.airbags = airbags;
        this.invoice = false;
        this.price = ThreadLocalRandom.current().nextInt(10000, 50000 + 1);
        this.vat = App.vatList.get(ThreadLocalRandom.current().nextInt(0, 3));
        this.date = new CustomDate();

        if(App.models.contains(this.model)){
            img = App.modelsMap.get(this.model).substring(25);
        }else{
            img = "/imgs/car.png";
        }
    }

    public int getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public String getColor() {
        return color;
    }

    public ArrayList<Airbag> getAirbags() {
        return airbags;
    }

    public int getPrice() {
        return price;
    }

    public int getVat() {
        return vat;
    }

    public CustomDate getDate() {
        return date;
    }

    public void setInvoice(boolean invoice) {
        this.invoice = invoice;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", model='" + model + '\'' +
                ", year=" + year +
                ", color='" + color + '\'' +
                ", airbags=" + airbags +
                '}';
    }
}

class IdToDelete{
    int id;

    public IdToDelete() {
    }

    public int getId() {
        return id;
    }
}

class InvoiceData{
    int year;
    int low;
    int high;

    public InvoiceData() {
    }

    public int getYear() {
        return year;
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }
}

class UpdatedData{
    int id;
    String model;
    int year;

    public int getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    @Override
    public String toString() {
        return "UpdatedData{" +
                "id=" + id +
                ", model='" + model + '\'' +
                ", year=" + year +
                '}';
    }
}
