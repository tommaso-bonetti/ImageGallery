package it.polimi.tiw.imagegallery.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.imagegallery.beans.Album;
import it.polimi.tiw.imagegallery.beans.Image;
import it.polimi.tiw.imagegallery.dao.AlbumDAO;
import it.polimi.tiw.imagegallery.dao.ImageDAO;
import it.polimi.tiw.imagegallery.utils.ConnectionManager;

@WebServlet("/GoToImagesPage")
public class GoToImagesPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	public GoToImagesPage() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionManager.getConnection(servletContext);

		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String albumIdString = StringEscapeUtils.escapeJava(request.getParameter("albumId"));
		int albumId;
		
		Integer userId = (Integer) request.getSession().getAttribute("userId");		
		if (userId == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing user session");
			return;
		}
		
		try {
			albumId = Integer.parseInt(albumIdString);
		} catch (NumberFormatException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid album id");
			return;
		}
		
		List<Image> images = null;
		ImageDAO imageDAO = new ImageDAO(connection);
		Album album = null;
		AlbumDAO albumDAO = new AlbumDAO(connection);
		
		try {
			images = imageDAO.fetchImagesNotInAlbum(albumId, userId);
			album = albumDAO.fetchAlbumById(albumId);
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in retrieving images from the database");
			return;
		}
		
		if (album == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid album selection");
			return;
		}
		
		// Redirect to the home page and add the albums to the parameters
		String path = "/WEB-INF/images.html";
		ServletContext servletContext = getServletContext();
		final WebContext context = new WebContext(request, response, servletContext, request.getLocale());
		context.setVariable("album", album);
		context.setVariable("images", images);
		templateEngine.process(path, context, response.getWriter());
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
