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

import it.polimi.tiw.imagegallery.dao.AlbumDAO;
import it.polimi.tiw.imagegallery.utils.ConnectionManager;

@WebServlet("/CreateAlbum")
public class CreateAlbum extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	public CreateAlbum() {
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
		String userIdString = StringEscapeUtils.escapeJava(request.getParameter("userId"));
		String albumTitle = StringEscapeUtils.escapeJava(request.getParameter("albumTitle"));
		
		try {
			if (userIdString == null || userIdString.isEmpty())
				throw new Exception("Missing user session");
			if (albumTitle == null || albumTitle.isBlank())
				throw new Exception("Missing or empty album title");
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		
		int userId;		
		try {
			userId = Integer.parseInt(userIdString);
		} catch (NumberFormatException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user id");
			return;
		}
		
		AlbumDAO albumDAO = new AlbumDAO(connection);
		
		try {
			albumDAO.createAlbum(userId, albumTitle);
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to create album");
			return;
		}
		
		response.sendRedirect(getServletContext().getContextPath() + "/GoToHomePage");
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
