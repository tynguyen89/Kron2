package api;

import org.hibernate.Session;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Query;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class Student extends User {


    @ManyToMany(mappedBy = "auditedStudents")
    private Set<Course> auditedCourse = new HashSet<>();

    @ManyToMany(mappedBy = "registeredStudents", cascade = CascadeType.PERSIST)
    private Set<Course> registeredCourse = new HashSet<>();


    @ManyToMany(mappedBy = "shoppingStudents")
    private Set<Course> shoppingCourse = new HashSet<>();

    /**
     * Default constructor
     */
    public Student() {
    }


    /**
     * give all the registered, audited and shopping courses
     *
     * @return List of Courses of registered, audited and shopping courses
     */
    public List<Course> getAllCourses() {
        List<Course> allCourses = new ArrayList<>();
        allCourses.addAll(getRegisteredCourse());
        allCourses.addAll(getAuditedCourse());
        allCourses.addAll(getShoppingCourse());
        return allCourses;
    }


    public static void main(String[] args) {
        Session session = MySession.getSession();
        Student student = session.get(Student.class, "siddharth16268@iiitd.ac.in");

//        System.out.println(student.hasAnyClashWithLecturesOf(session.get(Course.class, "ISI")));
    }

    /**
     *
     * @param name name(Like "Advanced Programming") of the course
     *             that needs to be registered list for this student
     * @return true if successful, otherwise false
     */
    public boolean insertRegisteredCourse(String name) {
        Course course = Course.getCourseByName(name);
        return insertRegisteredCourse(course);
    }

    /**
     *
     * @param name name(Like "Advanced Programming") of the course
     *             that needs to be added to Audited List for this student
     * @return true if successful, otherwise false
     */
    public boolean insertAuditedCourse(String name) {
        Course course = Course.getCourseByName(name);
        return insertAuditedCourse(course);
    }

    /**
     *
     * @param name name(Like "Advanced Programming") of the course
     *             that needs to be added to Shopping List for this student
     * @return true if successful, otherwise false
     */
    public boolean insertShoppingCourse(String name) {
        Course course = Course.getCourseByName(name);
        return insertShoppingCourse(course);
    }

    /**
     * Given a course, it adds as a shopping course
     *
     * @param course course that needs to be added
     * @return true if successful, otherwise false
     */
    public boolean insertShoppingCourse(Course course) {

        Session session = MySession.getSession();

        session.beginTransaction();
        this.getShoppingCourse().add(course);
        course.getShoppingStudents().add(this);
        session.saveOrUpdate(this);
        session.saveOrUpdate(course);
        session.getTransaction().commit();

        return true;
    }

    /**
     * Give a course, it adds as a registered course
     *
     * @param course course that needs to be added
     * @return true if successful
     */
    public boolean insertRegisteredCourse(Course course) {

        if (hasAnyCourseThatClashWith(course)) {
            System.out.println("Clashing");
            return false;
        }

        Session session = MySession.getSession();

        session.beginTransaction();
        this.getRegisteredCourse().add(course);
        course.getRegisteredStudents().add(this);
        session.saveOrUpdate(this);
        session.saveOrUpdate(course);
        session.getTransaction().commit();

        return true;
    }

    /**
     * Give a course, it adds as a audited course
     *
     * @param course course that needs to be added
     * @return true if successful
     */
    public boolean insertAuditedCourse(Course course) {

        if (hasAnyCourseThatClashWith(course)) {
            System.out.println("Clashing");
            return false;
        }

        Session session = MySession.getSession();

        session.beginTransaction();

        this.getAuditedCourse().add(course);
        course.getAuditedStudents().add(this);
        session.evict(this);
        session.saveOrUpdate(this);
        session.saveOrUpdate(course);
        session.getTransaction().commit();

        return true;
    }

    /**
     * Given a course, it adds from registered  course
     *
     * @param course course that needs to be deleted
     * @return true if successful, otherwise false
     */
    public boolean deleteShoppingCourse(Course course) {

        Session session = MySession.getSession();

        session.beginTransaction();
        this.getShoppingCourse().remove(course);
        course.getShoppingStudents().remove(this);
        session.saveOrUpdate(this);
        session.saveOrUpdate(course);
        session.getTransaction().commit();

        return true;
    }

    /**
     * Give a course, it deletes it from registered course
     *
     * @param course course that needs to be deleted
     * @return true if successful
     */
    public boolean deleteRegisteredCourse(Course course) {

        Session session = MySession.getSession();

        session.beginTransaction();
        this.getRegisteredCourse().remove(course);
        course.getRegisteredStudents().remove(this);
        session.saveOrUpdate(this);
        session.saveOrUpdate(course);
        session.getTransaction().commit();

        return true;
    }

    /**
     * Give a course, it removes from a audited courses
     *
     * @param course course that needs to be removed
     * @return true if successful
     */
    public boolean deleteAuditedCourse(Course course) {

        Session session = MySession.getSession();

        session.beginTransaction();

        this.getAuditedCourse().remove(course);
        course.getAuditedStudents().remove(this);
        session.evict(this);
        session.saveOrUpdate(this);
        session.saveOrUpdate(course);
        session.getTransaction().commit();

        return true;
    }
    /**
     * Finds if the given course has any clashes with the current lectures(of audited and registered courses only)
     * of the students. It only considers lecture od the given course
     * @param course Course with which the clashes need to be testes.
     * @return true if any lecture clashed
     */
    public boolean hasAnyCourseThatClashWith(Course course) {

        return course.getCourseEvents().stream().
                filter(CourseEvent::isLecture).
                filter(o -> isAnyLectureBetween(o.getStartTime(), o.getEndTime(), o.getDayOfWeek()))
                .count() != 0;

    }

    /**
     * Finds if student has any lecture(of audited and registered courses only) at the given time period
     * @param startTime start time of the period
     * @param endTime end time of the period
     * @param dayOfWeek DayOfWeek for that period
     * @return true if the student has any lecture from his registered or audited course in the given time span
     */
    public boolean isAnyLectureBetween(Time startTime, Time endTime, DayOfWeek dayOfWeek) {

        Session session = MySession.getSession();
        Query query = session.createQuery("select distinct event.course from CourseEvent as event " +
                "where (:thisStudent in elements(event.course.registeredStudents) " +
                "or :thisStudent in elements(event.course.auditedStudents))" +
                "and ( (event.startTime between :startT and :endT) or (event.startTime between :startT and :endT) )" +
                "and event.dayOfWeek = :EdayOfWeek and  event.eventType = 1");

        query.setParameter("thisStudent", this);
        query.setParameter("startT", startTime);
        query.setParameter("endT", endTime);
        query.setParameter("EdayOfWeek", dayOfWeek);

        return query.getResultList().size() != 0;

    }

    public boolean hasRegisteredForCourse(Course course) {
        return getRegisteredCourse().contains(course);
    }

    public boolean hasAuditedCourse(Course course) {
        return getAuditedCourse().contains(course);
    }

    public boolean hasShoppedForCourse(Course course) {
        return getShoppingCourse().contains(course);
    }


    /**
     * Creates an event request with the given specification and sets
     * isPending = true
     * isRejected = false
     * isCancelled = false
     * <p>
     * Checks if time is sensible
     * checks if room is free between the given time
     *
     * @param name        Event name
     * @param room        Room Object
     * @param description Description of the event
     * @param startTime   start time of the event
     * @param endTime     end time of the event
     * @param date        event date
     * @return true if event creation is successfull
     */
    public boolean createEventRequest(String name, Room room, String description, Time startTime, Time endTime, Date date) {

        Session session = MySession.getSession();

        if (new java.util.Date().before(startTime)) {
            System.out.println("Make a time machine first");
            return false;
        }
        if (startTime.after(endTime)) {
            System.out.println("Make a time machine first");
            return false;
        }
        if (!room.isFreeBetween(startTime, endTime, date)) {
            System.out.println("Clashing with events");
            return false;
        }
        Event event = new Event();
        event.setTagline(name);
        event.setDescription(description);

        event.setRoom(room);

        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setDate(date);
        event.setCancelled(false);
        event.setRejected(false);
        event.setPending(true);
        event.setCourseEvent(false);
        event.setCreationTime(new Timestamp(new java.util.Date().getTime()));
        event.setCreators(this);

        try {
            session.beginTransaction();
            session.saveOrUpdate(event);
            session.getTransaction().commit();

        } catch (Exception e) {

            System.out.println("Saving event failed because" + e.getMessage());
            return false;
        }

        return true;

    }


    public List<Event> getPendingEventRequests() {
        Session session = MySession.getSession();

        Query query = session.createQuery("FROM Event event where event.creators = :user" +
                " and event.isPending = :pending");
        query.setParameter("user", this);
        query.setParameter("pending", true);

        return (List<Event>) query.getResultList();
    }


    public Set<Course> getAuditedCourse() {
        return auditedCourse;
    }

    public void setAuditedCourse(Set auditedCourse) {
        this.auditedCourse = auditedCourse;
    }

    public Set<Course> getRegisteredCourse() {
        return registeredCourse;
    }

    public void setRegisteredCourse(Set<Course> registeredCourse) {
        this.registeredCourse = registeredCourse;
    }

    public Set<Course> getShoppingCourse() {
        return shoppingCourse;
    }

    public void setShoppingCourse(Set<Course> shoppingCourse) {
        this.shoppingCourse = shoppingCourse;
    }
}
