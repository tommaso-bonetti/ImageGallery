package it.polimi.tiw.imagegallery.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

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

@WebServlet("/AuthenticateUser")
public class AuthenticateUser extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public AuthenticateUser() {
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
		String username = null;
		String password = null;
		
		try {
			username = StringEscapeUtils.escapeJava(request.getParameter("username"));
			password = StringEscapeUtils.escapeJava(request.getParameter("password"));
			if (username == null || username.isEmpty())
				throw new Exception("Missing or empty username value");
			if (password == null || password.isEmpty())
				throw new Exception("Missing or empty password value");
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		
		UserDAO userDAO = new UserDAO(connection);
		User user = null;
		String path;
		
		try {
			if (userDAO.getUser(username) == null)
				throw new Exception("Username does not exist, register to create a new account");
			
			user = userDAO.checkCredentials(username, password);
			
			if (user == null)
				throw new Exception("The password is incorrect");
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to check user credentials");
			return;
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
			return;
		}
		
		request.getSession().setAttribute("userId", user.getId());
		path = getServletContext().getContextPath() + "/";
		response.sendRedirect(path);
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
