package it.polimi.tiw.imagegallery.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import it.polimi.tiw.imagegallery.dao.AlbumImagesDAO;
import it.polimi.tiw.imagegallery.utils.ConnectionManager;

@WebServlet("/AddToAlbum")
public class AddToAlbum extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	public AddToAlbum() {
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
		String redirectToAlbum = StringEscapeUtils.escapeJava(request.getParameter("redirectToAlbum"));
		String targetImage = StringEscapeUtils.escapeJava(request.getParameter("selectedImageId"));
		String targetAlbum = StringEscapeUtils.escapeJava(request.getParameter("targetAlbum"));

		Integer userId = (Integer) request.getSession().getAttribute("userId");		
		if (userId == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing user session");
			return;
		}
		
		try {
			if (targetImage == null || targetImage.isEmpty())
				throw new Exception("Missing target image");
			if (targetAlbum == null || targetAlbum.isEmpty())
				throw new Exception("Missing target album");
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		
		int targetImageId;
		int targetAlbumId;
		
		try {
			targetImageId = Integer.parseInt(targetImage);
			targetAlbumId = Integer.parseInt(targetAlbum);
		} catch (NumberFormatException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image or album selection");
			return;
		}
		
		AlbumImagesDAO albumImagesDAO = new AlbumImagesDAO(connection);
		
		try {
			if (!albumImagesDAO.checkAlbumExistence(targetAlbumId))
				throw new Exception("Nonexistent album");
			
			if (!albumImagesDAO.checkImageExistence(targetImageId))
				throw new Exception("Nonexistent image");
			
			if (!albumImagesDAO.checkAlbumOwnership(userId, targetAlbumId))
				throw new Exception("Cannot add image to album owned by another user");
			
			if (!albumImagesDAO.checkImageOwnership(userId, targetImageId))
				throw new Exception("Cannot add image owned by another user to your album");
			
			albumImagesDAO.addImageToAlbum(targetImageId, targetAlbumId);
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to add image to album");
			return;
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
			return;
		}
		
		String path;
		
		if (Objects.equals(redirectToAlbum, "true")) {
			String originAlbumId = request.getParameter("originAlbumId");
			String page = request.getParameter("page");
			
			path = String.format(
					"/GoToAlbumPage?albumId=%s&page=%s&selectedImageId=%s",
					URLEncoder.encode((originAlbumId == null ? targetAlbum : originAlbumId), "UTF-8"),
					URLEncoder.encode((page == null ? "1" : page), "UTF-8"),
					URLEncoder.encode(targetImage, "UTF-8"));
			
			if (request.getParameter("selectedImageId") != null)
				path += String.format("", URLEncoder.encode(request.getParameter("selectedImageId"), "UTF-8"));
		} else {
			path = String.format("/GoToImagesPage?albumId=%s", URLEncoder.encode(targetAlbum, "UTF-8"));
		}
		
		response.sendRedirect(getServletContext().getContextPath() + path);
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
