/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.issue.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.Protobuf.setNullable;

public class DeleteCommentActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();

  private IssueDbTester issueDbTester = new IssueDbTester(dbTester);

  private OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);

  private WsActionTester tester = new WsActionTester(
    new DeleteCommentAction(userSession, dbClient, new IssueFinder(dbClient, userSession), responseWriter));

  @Test
  public void delete_comment() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, "john", "please fix it");
    userSession.login("john").addProjectUuidPermissions(USER, issueDto.getProjectUuid());

    call(commentDto.getKey());

    verify(responseWriter).write(eq(issueDto.getKey()), any(Request.class), any(Response.class));
    assertThat(dbClient.issueChangeDao().selectCommentByKey(dbTester.getSession(), commentDto.getKey())).isNotPresent();
  }

  @Test
  public void delete_comment_using_deprecated_key_parameter() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, "john", "please fix it");
    userSession.login("john").addProjectUuidPermissions(USER, issueDto.getProjectUuid());

    tester.newRequest().setParam("key", commentDto.getKey()).setParam("text", "please have a look").execute();

    verify(responseWriter).write(eq(issueDto.getKey()), any(Request.class), any(Response.class));
    assertThat(dbClient.issueChangeDao().selectCommentByKey(dbTester.getSession(), commentDto.getKey())).isNotPresent();
  }

  @Test
  public void fail_when_comment_does_not_belong_to_current_user() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, "john", "please fix it");
    userSession.login("another").addProjectUuidPermissions(USER, issueDto.getProjectUuid());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("You can only delete your own comments");
    call(commentDto.getKey());
  }

  @Test
  public void fail_when_comment_has_not_user() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, null, "please fix it");
    userSession.login("john").addProjectUuidPermissions(USER, issueDto.getProjectUuid());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("You can only delete your own comments");
    call(commentDto.getKey());
  }

  @Test
  public void fail_when_missing_comment_key() throws Exception {
    userSession.login("john");

    expectedException.expect(IllegalArgumentException.class);
    call(null);
  }

  @Test
  public void fail_when_comment_does_not_exist() throws Exception {
    userSession.login("john");

    expectedException.expect(NotFoundException.class);
    call("ABCD");
  }

  @Test
  public void fail_when_not_authenticated() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    call("ABCD");
  }

  @Test
  public void fail_when_not_enough_permission() throws Exception {
    IssueDto issueDto = issueDbTester.insertIssue();
    IssueChangeDto commentDto = issueDbTester.insertComment(issueDto, "john", "please fix it");
    userSession.login("john").addProjectUuidPermissions(CODEVIEWER, issueDto.getProjectUuid());

    expectedException.expect(ForbiddenException.class);
    call(commentDto.getKey());
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo("delete_comment");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(1);
    assertThat(action.responseExample()).isNull();
  }

  private TestResponse call(@Nullable String commentKey) {
    TestRequest request = tester.newRequest();
    setNullable(commentKey, comment -> request.setParam("comment", comment));
    return request.execute();
  }

}
