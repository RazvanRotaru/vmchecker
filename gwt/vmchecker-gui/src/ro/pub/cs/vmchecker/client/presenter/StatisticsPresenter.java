package ro.pub.cs.vmchecker.client.presenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ro.pub.cs.vmchecker.client.i18n.VmcheckerConstants;
import ro.pub.cs.vmchecker.client.event.StatusChangedEvent;
import ro.pub.cs.vmchecker.client.model.Assignment;
import ro.pub.cs.vmchecker.client.model.AccountType;
import ro.pub.cs.vmchecker.client.model.EvaluationResult;
import ro.pub.cs.vmchecker.client.model.ResultInfo;
import ro.pub.cs.vmchecker.client.model.User;
import ro.pub.cs.vmchecker.client.service.HTTPService;
import ro.pub.cs.vmchecker.client.util.AlphanumComparator;
import ro.pub.cs.vmchecker.client.util.ANSITextColorFilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasWidgets;

public class StatisticsPresenter implements Presenter {

	public interface Widget {
		HTMLTable getTeamTable();
		HTMLTable getStudentTable();
		void displayInfo(User user, Assignment[] assignments,
			ArrayList<ResultInfo> teamResultInfo, ArrayList<ResultInfo> studentResultInfo);
		void displayResultDetails(String account, String assignment, String result, String htmlDetails);
	}

	private class TableClickHandler implements ClickHandler {
		final HTMLTable table;
		final ArrayList<ResultInfo> resultsInfo;

		public TableClickHandler(HTMLTable table, ArrayList<ResultInfo> resultsInfo) {
			this.table = table;
			this.resultsInfo = resultsInfo;
		}

		@Override
		public void onClick(ClickEvent event) {
			HTMLTable.Cell cell = this.table.getCellForEvent(event);
			if (cell != null) {
				GWT.log("Click for cell " + cell, null);
				ResultInfo resultInfo = this.resultsInfo.get(cell.getRowIndex() - 1);
				String assignmentId = assignments[cell.getCellIndex() - 1].id;
				if (resultInfo.results.containsKey(assignmentId)) {
					loadAndShowResultDetails(resultInfo, assignmentId);
				}
			}
		}
	}

	private class ResultInfoComparator implements Comparator<ResultInfo> {
		AlphanumComparator stringComparator = new AlphanumComparator();
		@Override
		public int compare(ResultInfo r1, ResultInfo r2) {
			return stringComparator.compare(r1.accountName, r2.accountName);
		}
	}

	private EventBus eventBus;
	private HTTPService service;
	private static VmcheckerConstants constants = GWT
			.create(VmcheckerConstants.class);
	private HasWidgets container;
	private StatisticsPresenter.Widget widget;

	private String courseId;
	private User user;
	private Assignment[] assignments;
	private ArrayList<ResultInfo> teamResultsInfo, studentResultsInfo;

	public StatisticsPresenter(EventBus eventBus, HTTPService service,
			String courseId, User user, Assignment[] assignments, StatisticsPresenter.Widget widget) {
		this.eventBus = eventBus;
		this.service = service;
		this.courseId = courseId;
		this.assignments = assignments;
		this.user = user;
		this.teamResultsInfo = new ArrayList<ResultInfo>();
		this.studentResultsInfo = new ArrayList<ResultInfo>();
		bindWidget(widget);
		listenTableEvents();
	}

	private void listenTableEvents() {
		widget.getTeamTable().addClickHandler(new TableClickHandler(widget.getTeamTable(), teamResultsInfo));
		widget.getStudentTable().addClickHandler(new TableClickHandler(widget.getStudentTable(), studentResultsInfo));
	}

	private void loadAndShowResultDetails(final ResultInfo resultInfo, final String assignment) {
		eventBus.fireEvent(new StatusChangedEvent(StatusChangedEvent.StatusType.ACTION,
				constants.loadResults()));
		service.getResults(courseId, assignment, resultInfo.accountName,
				resultInfo.accountType, new AsyncCallback<EvaluationResult[]> () {

			@Override
			public void onFailure(Throwable caught) {
				GWT.log("StatisticsPresenter.loadAndShowResultDetails()", caught);
			}

			@Override
			public void onSuccess(EvaluationResult[] result) {
				eventBus.fireEvent(new StatusChangedEvent(StatusChangedEvent.StatusType.RESET, null));
				String resultsHTML = "";
				for (int i = 0; i < result.length; i++) {
					resultsHTML += result[i].toHTML();
				}
				resultsHTML = ANSITextColorFilter.apply(resultsHTML);
				widget.displayResultDetails(resultInfo.accountName, assignment,
						resultInfo.results.get(assignment), resultsHTML);
			}

		});
	}

	private void bindWidget(StatisticsPresenter.Widget widget) {
		this.widget = widget;
	}

	@Override
	public void clearEventHandlers() {

	}

	private void processResults(ResultInfo[] resultsInfo) {
		studentResultsInfo.clear();
		teamResultsInfo.clear();
		for (ResultInfo result : resultsInfo) {
			if (result.accountType == AccountType.USER) {
				studentResultsInfo.add(result);
			}

			if (result.accountType == AccountType.TEAM) {
				teamResultsInfo.add(result);
			}
		}
		Collections.sort(studentResultsInfo, new ResultInfoComparator());
		Collections.sort(teamResultsInfo, new ResultInfoComparator());
	}

	@Override
	public void go(final HasWidgets container) {
		this.container = container;
		eventBus.fireEvent(new StatusChangedEvent(StatusChangedEvent.StatusType.ACTION,
				constants.loadStatistics()));
		service.getAllResults(courseId, new AsyncCallback<ResultInfo[]>() {

			@Override
			public void onFailure(Throwable caught) {
				GWT.log("StatisticsPresenter.getAllResults()", caught);
			}

			@Override
			public void onSuccess(ResultInfo[] result) {
				container.clear();
				processResults(result);
				widget.displayInfo(user, assignments, teamResultsInfo, studentResultsInfo);
				container.add((com.google.gwt.user.client.ui.Widget) widget);
				eventBus.fireEvent(new StatusChangedEvent(StatusChangedEvent.StatusType.RESET, null));
			}

		});

	}

}
