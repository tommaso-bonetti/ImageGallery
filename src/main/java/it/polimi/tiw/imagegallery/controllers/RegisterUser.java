package it.polimi.tiw.imagegallery.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import it.polimi.tiw.imagegallery.beans.User;
import it.polimi.tiw.imagegallery.dao.UserDAO;
import it.polimi.tiw.imagegallery.utils.ConnectionManager;

@WebServlet("/RegisterUser")
public class RegisterUser extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public RegisterUser() {
		super();
	}
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionManager.getConnection(servletContext);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String email = null;
		String username = null;
		String password = null;
		String repeatPassword = null;
		
		Pattern emailPattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_.-]+[a-zA-Z0-9]@[a-zA-Z][a-zA-Z0-9.-]+[a-zA-Z]$");
		Pattern usernamePattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_.-]*$");
		
		try {
			email = StringEscapeUtils.escapeJava(request.getParameter("email"));
			username = StringEscapeUtils.escapeJava(request.getParameter("username"));
			password = StringEscapeUtils.escapeJava(request.getParameter("password"));
			repeatPassword = StringEscapeUtils.escapeJava(request.getParameter("repeatPassword"));
			if (email == null || email.isEmpty())
				throw new Exception("Missing or empty email value");
			if (username == null || username.isEmpty())
				throw new Exception("Missing or empty username value");
			if (password == null || password.isEmpty())
				throw new Exception("Missing or empty password value");
			if (repeatPassword == null || repeatPassword.isEmpty())
				throw new Exception("Missing or empty repeat password value");
		} catch (Exception e) {
			System.out.println("Bad request: " + e.getMessage());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		
		String path;
		List<String> errorMessages = new ArrayList<>();
		
		if (!emailPattern.matcher(email).matches()) {
			System.out.println("Invalid email format");
			errorMessages.add("Invalid email format");
		}
		if (!usernamePattern.matcher(username).matches()) {
			System.out.println("Username can only contain letters, numbers, underscores, dots and hyphens, must start with a letter");
			errorMessages.add("Username can only contain letters, numbers, underscores, dots and hyphens, must start with a letter");
		}
		if (password.length() < 6) {
			System.out.println("Password needs to be at least 6 characters long");
			errorMessages.add("Password needs to be at least 6 characters long");
		}
		if (!password.equals(repeatPassword)) {
			System.out.println("Password and repeat password do not match");
			errorMessages.add("Password and repeat password do not match");
		}
		
		if (errorMessages.size() > 0) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.join("; ", errorMessages));
		} else {
			UserDAO userDAO = new UserDAO(connection);
			User user = null;
			
			try {
				user = userDAO.createUser(email, username, password);
			} catch (SQLException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to register new user");
				return;
			} catch (Exception e) {
				response.sendError(HttpServletResponse.SC_CONFLICT, e.getMessage());
				return;
			}
			
			request.getSession().setAttribute("userId", user.getId());
			path = getServletContext().getContextPath() + "/";
			response.sendRedirect(path);
		}
	}

	@Override
	public void destroy() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e){
				e.printStackTrace();
			}
		}
	}
}
