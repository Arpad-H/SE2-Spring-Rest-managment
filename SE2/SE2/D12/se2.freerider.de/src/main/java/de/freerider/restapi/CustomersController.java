package de.freerider.restapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.freerider.datamodel.Customer;
import de.freerider.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@RestController
class CustomersController implements CustomersAPI {
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ApplicationContext context;
    //
    private final ObjectMapper objectMapper;
    //
    private final HttpServletRequest request;

    public CustomersController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @Override
    public ResponseEntity<List<?>> getCustomers() {
        ResponseEntity<List<?>> re = null;
        System.err.println(request.getMethod() + " " + request.getRequestURI());
        try {
            ArrayNode arrayNode = peopleAsJSON();
            ObjectReader reader = objectMapper.readerFor(new TypeReference<List<ObjectNode>>() {
            });
            List<String> list = reader.readValue(arrayNode);
            //
            re = new ResponseEntity<List<?>>(list, HttpStatus.OK);

        } catch (IOException e) {
            re = new ResponseEntity<List<?>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return re;
    }

    @Override
    public ResponseEntity<?> getCustomer(long id) {
        for (JsonNode p : peopleAsJSON()) {
            if (p.get("id").asLong() == id) {
                return new ResponseEntity<String>(p.toString(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<List<?>> postCustomers(Map<String, Object>[] jsonMap) {
        System.err.println("POST /customers");
        if (jsonMap == null)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
//
        System.out.println("[{");
        for (Map<String, Object> kvpairs : jsonMap) {
            kvpairs.keySet().forEach(key -> {
                Object value = kvpairs.get(key);
                System.out.println(" [ " + key + ", " + value + " ]");
            });
            Optional newCustomer = null;
            try {
                newCustomer = accept(kvpairs);
            } catch (ConflictException e) {
                return new ResponseEntity<>(null,HttpStatus.CONFLICT);
            }
            if (newCustomer.isPresent()) {

                customerRepository.save((Customer) newCustomer.get());
            }else{
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        }
        System.out.println("}]");
        return new ResponseEntity<>(null, HttpStatus.CREATED);
    }

    private Optional<Customer> accept(Map<String, Object> kvpairs) throws ConflictException{
        Long newID;
        if (!kvpairs.containsKey("id")) {
            do {
                newID = generateID();
            } while (customerRepository.existsById(newID));
        } else {
            newID = Long.valueOf( kvpairs.get("id").toString());
        }
        if (customerRepository.existsById(newID)) throw new ConflictException();
        if (newID <= 0) {
            return Optional.empty();
        }
        if (!kvpairs.containsKey("name") || !kvpairs.containsKey("first")) {
            return Optional.empty();
        }
        Customer newCustomer = new Customer()
                .setId(newID)
                .setName(kvpairs.get("first").toString(), kvpairs.get("name").toString());
        if (kvpairs.containsKey("contacts")) {
            for (String s : kvpairs.get("contacts").toString().split(";")) {
                newCustomer.addContact(s);
            }
        }
        return Optional.of(newCustomer);
    }

    private Long generateID() {
        return (long) (Math.random() * 1000000);
    }

    @Override
    public ResponseEntity<List<?>> putCustomers(Map<String, Object>[] jsonMap) {
        return null;
    }

    @Override
    public ResponseEntity<?> deleteCustomer(long id) {
        System.err.println("DELETE /customers/" + id);
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return new ResponseEntity<>(null, HttpStatus.ACCEPTED); // status 202
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }


    private ArrayNode peopleAsJSON() {
        //
        ArrayNode arrayNode = objectMapper.createArrayNode();
        //
        customerRepository.findAll().forEach(c -> {
            StringBuffer sb = new StringBuffer();
            c.getContacts().forEach(contact -> sb.append(sb.length() == 0 ? "" : "; ").append(contact));
            arrayNode.add(
                    objectMapper.createObjectNode()
                            .put("name", c.getLastName())
                            .put("first", c.getFirstName())
                            .put("contacts", sb.toString())
                            .put("id", c.getId())
            );
        });
        return arrayNode;
    }
//    /*
//     * Quick Person class
//     */
//    class Person {
//        String firstName = "";
//        String lastName = "";
//        final List<String> contacts = new ArrayList<String>();
//        int id = -1;
//
//        CustomersController.Person setId(int id) {
//            this.id = id;
//            return this;
//        }
//
//        CustomersController.Person setName(String firstName, String lastName ) {
//            this.firstName = firstName;
//            this.lastName = lastName;
//            return this;
//        }
//
//        CustomersController.Person addContact(String contact ) {
//            this.contacts.add( contact );
//            return this;
//        }
//    }
//
//    private final CustomersController.Person eric = new CustomersController.Person()
//            .setName( "Eric", "Meyer" )
//            .addContact( "eric98@yahoo.com" )
//            .addContact( "(030) 3945-642298" )
//            .setId(1);
//    //
//    private final CustomersController.Person anne = new CustomersController.Person()
//            .setName( "Anne", "Bayer" )
//            .addContact( "anne24@yahoo.de" )
//            .addContact( "(030) 3481-23352" )
//            .setId(2);
//    //
//    private final CustomersController.Person tim = new CustomersController.Person()
//            .setName( "Tim", "Schulz-Mueller" )
//            .addContact( "tim2346@gmx.de" )
//            .setId(3);
//
//    private final List<CustomersController.Person> people = Arrays.asList( eric, anne, tim );
//
//
//    private ArrayNode peopleAsJSON() {
//        //
//        ArrayNode arrayNode = objectMapper.createArrayNode();
//        //
//        people.forEach( c -> {
//            StringBuffer sb = new StringBuffer();
//            c.contacts.forEach( contact -> sb.append( sb.length()==0? "" : "; " ).append( contact ) );
//            arrayNode.add(
//                    objectMapper.createObjectNode()
//                            .put( "name", c.lastName )
//                            .put( "first", c.firstName )
//                            .put( "contacts", sb.toString() )
//                            .put("id",c.id)
//            );
//        });
//        return arrayNode;
//    }
}
