package server;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

//Query Parameters: student name, student email, student year, class_id

public class AddStudentHandler implements Route{
  @Override
  public Object handle(Request request, Response response) {
    QueryParamsMap qm = request.queryMap();

    // If the query parameters are valid ...
    if (qm.hasKey("studentName") && qm.hasKey("email") && qm.hasKey("year") && qm.hasKey("className")){
      String studentName = qm.value("studentName");
      String studentEmail = qm.value("email");
      String studentYear = qm.value("year");
      String className = qm.value("className");

      return handleTables(studentName, studentEmail, studentYear, className);

    } else {
      return "failure with the provided query parameters";
    }
  }

  public List<String> handleTables(String studentName, String studentEmail, String studentYear, String className){
    List<String> studentWaitlist = new ArrayList<String>();

    try {
      Class.forName("org.sqlite.JDBC");
      String urlToDB = "jdbc:sqlite:" + "waitlist.sqlite3";
      Connection conn = DriverManager.getConnection(urlToDB);
      Statement stat = conn.createStatement();
      stat.executeUpdate("PRAGMA foreign_keys=ON;");
      PreparedStatement prep;
      ResultSet rs;

      // Check if student already exists in "students" table
      prep = conn.prepareStatement("select * from students WHERE name = ?");
      prep.setString(1, studentName);
      rs = prep.executeQuery();
      Boolean studentExistsInStudents = false;
      Integer studentID = 0;

      while(rs.next()){
        studentExistsInStudents = true;
        studentID = rs.getInt(1);
      }

      if(studentExistsInStudents){
        System.out.println("student " + studentName + " already exists in 'students table");
      }

      // If student doesn't exist in "students" table, add student to it
      Integer maxStudentID = 0;
      if(studentExistsInStudents == false){
        System.out.println("student " + studentName + " does not exist in 'students' table");
        // Find max student_id
        prep = conn.prepareStatement("SELECT MAX(student_id) from students");
        rs = prep.executeQuery();
        while(rs.next()){
          maxStudentID = rs.getInt(1);
          studentID = maxStudentID + 1;
        }

        // Insert new student into "students" table
        /*
        prep = conn.prepareStatement(
            "INSERT INTO students VALUES (?, ?, ?, ?);");

        prep.setInt(1, studentID);
        prep.setString(2, studentName);
        prep.setString(3, studentEmail);
        prep.setString(4, studentYear);
        prep.addBatch();
        prep.executeBatch();
         */
        System.out.println("student " + studentName + " was added to 'students' table at " + studentID);
      }

      // Get class_id from "classes" table
      prep = conn.prepareStatement("select * from classes WHERE title = ?");
      prep.setString(1, className);
      rs = prep.executeQuery();
      String classID = "";

      while(rs.next()){
        classID = rs.getString(1);
      }
      System.out.println("the class " + className + " has classID " + classID);

      // Check if student exists in "enrollments" table
      prep = conn.prepareStatement("select * from enrollments WHERE student_id = ?");
      prep.setString(1, studentID.toString());
      rs = prep.executeQuery();
      Boolean studentExistsInEnrollmentsWithCorrectClass = false;

      while(rs.next()){
        String currClassID = rs.getString(3);
        if(currClassID.equals(classID)){
          studentExistsInEnrollmentsWithCorrectClass = true;
        }
      }

      // If student doesn't exist in "enrollments" with this specific course then add them
      Integer enrollID = 0;
      if(studentExistsInEnrollmentsWithCorrectClass == false){
        System.out.println("student "+ studentName + " does not exist correctly with class " + className + " in 'enrollments'");
        System.out.println("so, studentID " + studentID + " does not exist correctly with class " + classID + " in 'enrollments'");

        // Find max enrollment_id
        prep = conn.prepareStatement("SELECT MAX(enroll_id) from enrollments");
        rs = prep.executeQuery();
        while(rs.next()){
          enrollID = rs.getInt(1);
          enrollID = enrollID + 1;
        }

        // Insert new student/course pair into 'enrollments' table
        /*
        prep = conn.prepareStatement(
        "INSERT INTO enrollments VALUES (?, ?, ?);");

        prep.setInt(1, enrollID);
        prep.setInt(2, studentID);
        prep.setInt(3, classID);
        prep.addBatch();
        prep.executeBatch();
         */

        System.out.println("studentID " + studentID + " and classID " + classID + " were added to 'enrollments' at enrollID " + enrollID);
      }

      // Return: list of names of students in waitlist in order for that specific course
      prep = conn.prepareStatement("select * from enrollments WHERE class_id = ?");
      prep.setString(1, classID);
      rs = prep.executeQuery();

      List<String> studentIDList = new ArrayList<String>();
      while(rs.next()){
        studentIDList.add(rs.getString(2));
      }
      System.out.println("created a list of relevant student IDs: " + studentIDList);

      prep = conn.prepareStatement("select * from students WHERE student_id = ?");
      for (int i = 0; i < studentIDList.size(); i++){
        prep.setString(1, studentIDList.get(i));
        rs = prep.executeQuery();

        while(rs.next()){
          studentWaitlist.add(rs.getString(2));
        }
      }
      System.out.println("created list of student names to be returned: " + studentWaitlist);

    } catch (SQLException e){
      System.out.println("caught this exception: " + e);
    } catch (ClassNotFoundException f){
      System.out.println("caught this exception: " + f);
    }

    return studentWaitlist;


    // If the student doesn't exist in "students" table, add the student there
    // Get the class id from the "classes" table using the class name
    // If the student's ID doesn't exist in the "enrollments" table, add the student there with the class_ID
    // Return: list of names of students in waitlist in order for that specific course

  }

  private String getClassID(PreparedStatement prep, Connection conn, ResultSet rs, String className)
      throws SQLException {
    prep = conn.prepareStatement("select * from classes WHERE title = ?");
    prep.setString(1, className);
    rs = prep.executeQuery();
    String classID = "";

    while (rs.next()) {
      classID = rs.getString(1);
    }
    return classID;
  }
}
