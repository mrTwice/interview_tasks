package ru.infinitesynergy.yampolskiy.restapiserver.server.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.infinitesynergy.yampolskiy.restapiserver.entities.User;
import ru.infinitesynergy.yampolskiy.restapiserver.exceptions.NotValidMethodException;
import ru.infinitesynergy.yampolskiy.restapiserver.exceptions.UserNotFoundException;
import ru.infinitesynergy.yampolskiy.restapiserver.jwt.BearerAuthentication;
import ru.infinitesynergy.yampolskiy.restapiserver.jwt.JwtUtils;
import ru.infinitesynergy.yampolskiy.restapiserver.server.http.*;
import ru.infinitesynergy.yampolskiy.restapiserver.service.UserService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SingInRoute implements Route{
    private UserService userService;
    private ObjectMapper objectMapper = new ObjectMapper();

    public SingInRoute(UserService userService) {
        this.userService = userService;
    }

    @Override
    public HttpResponse execute(HttpRequest httpRequest) throws JsonProcessingException {
        if (!httpRequest.getMethod().equals(HttpMethod.GET)) {
            throw new NotValidMethodException("Некорректный метод запроса: " + httpRequest.getMethod());
        }
        String stringUserDTO = httpRequest.getBody();
        User user = objectMapper.readValue(stringUserDTO, User.class);
        User existUser = userService.getUserByUserName(user.getLogin());
        if (existUser == null || !existUser.getPassword().equals(user.getPassword())) {
            throw new UserNotFoundException("Неверно указан логин или пароль.");
        }
        String jwtToken = JwtUtils.createToken(user.getLogin());
        BearerAuthentication bearerAuth = new BearerAuthentication(jwtToken);
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setProtocolVersion(httpRequest.getProtocolVersion());
        httpResponse.setStatus(HttpStatus.OK);
        HttpHeaders headers = new HttpHeaders();
        headers.addHeader(HttpHeader.DATE.getHeaderName(), ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME));
        headers.addHeader(HttpHeader.SERVER.getHeaderName(), "BankServer/0.1");
        headers.addHeader(HttpHeader.CONTENT_TYPE.getHeaderName(), "application/json");
        headers.addHeader(HttpHeader.AUTHORIZATION.getHeaderName(), bearerAuth.getJwtToken());
        httpResponse.setHeaders(headers);
        return httpResponse;
    }
}
