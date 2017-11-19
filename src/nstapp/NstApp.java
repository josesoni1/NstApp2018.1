package nstapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.json.simple.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
/**
 *
 * @author precision
 */
public class NstApp {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Introduce tu correo @aiesec.net o hosteado en google");
        String email = sc.nextLine();
        System.out.println("Introduce tu password");
        String password = sc.nextLine();
        System.out.println("Es importante que actives las aplicaciones poco seguras para que podamos hacer el envío del correo");
        
        ArrayList<String[]> eps = new ArrayList<String[]>();
        String token = "e316ebe109dd84ed16734e5161a2d236d0a7e6daf499941f7c110078e3c75493";// token publico en YOP
        Date fin = new Date();
        Date init = new Date(fin.getYear()-1,fin.getMonth(),fin.getDate());
        String direction = "https://gis-api.aiesec.org/v2/people?access_token="+token+"&per_page=100&filters[registered]%5Bfrom%5D="+init.getDate()+"-"+(init.getMonth()+1)+"-"+(init.getYear()+1900)+"&filters[registered]%5Bto%5D="+fin.getDate()+"-"+(fin.getMonth()+1)+"-"+(fin.getYear()+1900);
         URL url = null;
            try {
                url = new URL(direction);
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
            String res = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
                for (String line; (line = reader.readLine()) != null;) {
                    //System.out.println(line);
                    res +=line;
                }
            }       catch (IOException ex) {
                ex.printStackTrace();
            }
            String pages = res.substring(res.indexOf("total_pages")+13, res.indexOf("}"));
            System.out.println(" : "+pages);
            int pag = Integer.parseInt(pages);
            pages = res.substring(res.indexOf("current_page")+14, res.indexOf("total_pages")-2);
            System.out.println(pages+" : "+pag);
            int epes = Integer.parseInt(pages);
            //Consigo Keys
            JSONParser jsonParser;
                jsonParser = new JSONParser();
                JSONObject jsonObject;
            try {
                jsonObject = (JSONObject) jsonParser.parse(new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")));
            } catch (Exception ex) {
                //Logger.getLogger(NstApp.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
                jsonObject = new JSONObject() ;
            }           
            Set keys = jsonObject.keySet();
                //System.out.println(keys);
                Object dat = keys.toArray()[0];
                JSONArray content = (JSONArray)jsonObject.get(dat);
                //System.out.println(content.size());
                JSONObject content1 = (JSONObject) content.get(0);
                keys = content1.keySet();
                Object[] idkeyArr = new Object[content1.size()];
                idkeyArr = content1.keySet().toArray();
                Object idKey = null;
                Object mangersKey = null;
                Object nameKey = null;
                Object mailKey = null;
                for(int j = 0 ; j< idkeyArr.length; j++){
                    if(idkeyArr[j].toString().equals("id"))
                        idKey = idkeyArr[j];
                    if(idkeyArr[j].toString().equals("full_name"))
                        nameKey = idkeyArr[j];
                    if(idkeyArr[j].toString().equals("email"))
                        mailKey = idkeyArr[j];
                    if(idkeyArr[j].toString().equals("managers"))
                        mangersKey = idkeyArr[j];
                    
                }
            //Ya acabe de conseguir Keys
            //Inicio proceso iterativo
            pag = 10;
            for(int i = 0; i < pag; i++){
                direction = "https://gis-api.aiesec.org/v2/people?access_token="+token+"&page="+(i+1)+"&per_page=100&filters[registered]%5Bfrom%5D="+init.getDate()+"-"+(init.getMonth()+1)+"-"+(init.getYear()+1900)+"&filters[registered]%5Bto%5D="+fin.getDate()+"-"+(fin.getMonth()+1)+"-"+(fin.getYear()+1900);
                try {
                    url = new URL(direction);
                } catch (MalformedURLException ex) {
                    ex.printStackTrace();
                }              
                try {
                    jsonObject = (JSONObject) jsonParser.parse(new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    jsonObject = new JSONObject() ;
                }
                content = (JSONArray)jsonObject.get(dat);
                for (int k = 0; k < content.size(); k++){
                    String id, name, mail, managers;
                    id =(((JSONObject) content.get(k)).get(idKey)).toString();
                    name =(((JSONObject) content.get(k)).get(nameKey)).toString();
                    mail =(((JSONObject) content.get(k)).get(mailKey)).toString();
                    managers =(((JSONObject) content.get(k)).get(mangersKey)).toString();
                    //System.out.println(managers);
                    if(managers.length()>50){
                        int namesta =managers.lastIndexOf("\"full_name\":\"")+13;
                        int namefin = managers.indexOf("\"",namesta);
                        int mailsta = managers.lastIndexOf("\"email\":\"")+9;
                        int mailfin = managers.indexOf("\"",mailsta);
                        String manName = managers.substring(namesta, namefin);
                        String manMail = managers.substring(mailsta,mailfin);
                        String manager = manName +" ; " + manMail;
                        //System.out.println(manager);
                        String[] ep = new String[4];
                        ep[0] = id;
                        ep[1] = name;
                        ep[2] = mail;
                        ep[3] = manager;
                        eps.add(ep);
                    }
                }
                System.out.println("Page:"+i+" EPS: " + eps.size());
            }
            //Mando el correo
            String fromEmail = email;
            Session session = login(fromEmail,password);
            String mailContent = "<table> <tr><th>ID</th><th>Name</th><th>Mail</th><th>EP Manager</th></tr>";
            for(int j = 0; j< eps.size(); j++){
                mailContent = mailContent + "<tr><td>"+eps.get(j)[0]+"</td><td>"+eps.get(j)[1]+"</td><td>"+eps.get(j)[2]+"</td><td>"+eps.get(j)[3]+"</td></tr>";
            }
            mailContent = mailContent + "</table>";
            try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromEmail));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse("jose.soni@aiesec.net, sgarcia@aiesec.org.mx"));
			message.setSubject("Info de los EPs e EP Managers");
			message.setText(mailContent,"UTF-8","html");
			Transport.send(message);
			System.out.println("Done");
		} catch (MessagingException e) {
			e.printStackTrace();
		}
        }
        
    public static Session login(String fromEmail, String password){
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
        props.put("mail.smtp.port", "587"); //TLS Port
        props.put("mail.smtp.auth", "true"); //enable authentication
        props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS
        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
                //override the getPasswordAuthentication method
                protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(fromEmail, password);
                }
        };
        Session session = Session.getInstance(props, auth);
        return session;
    }
}
