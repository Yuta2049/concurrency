package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.Mockito.*;

public class MountTableRefresherServiceTests {

    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager successManager;

    private Others.MountTableManager failedManager;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = new MountTableRefresherService();
        service.setCacheUpdateTimeout(1000);
        routerStore = mock(Others.RouterStore.class);
        successManager = mock(Others.MountTableManager.class);
        failedManager = mock(Others.MountTableManager.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
        // service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
        // service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(successManager.refresh()).thenReturn(true);

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        // smth more
        when(mockedService.getRefresher(anyString(), anyString()))
                .thenReturn(new MountTableRefresher(successManager, returnsSecondArg().toString()));

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(failedManager.refresh()).thenReturn(false);

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        // smth more
        when(mockedService.getRefresher(anyString(), anyString()))
                .thenReturn(new MountTableRefresher(failedManager, returnsSecondArg().toString()));

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(4)).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSuccessedTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(successManager.refresh()).thenReturn(true);
        when(failedManager.refresh()).thenReturn(false);

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        // smth more
        when(mockedService.getRefresher(Mockito.argThat("local"::equals), anyString()))
                .thenReturn(new MountTableRefresher(successManager, returnsSecondArg().toString()));

        when(mockedService.getRefresher(Mockito.argThat(arg -> !"local".equals(arg)), anyString()))
                .thenReturn(new MountTableRefresher(failedManager, returnsSecondArg().toString()));

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache, times(2)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(successManager.refresh()).thenReturn(true);
        when(failedManager.refresh()).thenThrow(RuntimeException.class);

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        // smth more
        when(mockedService.getRefresher(anyString(), anyString()))
                .thenReturn(new MountTableRefresher(failedManager, returnsSecondArg().toString()))
                .thenReturn(new MountTableRefresher(successManager, returnsSecondArg().toString()));
        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    public void interruptedExceptionInOneTask() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(successManager.refresh()).thenReturn(true);
        when(failedManager.refresh()).thenAnswer(invocation -> {
            throw new InterruptedException();
        });

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        // smth more
        when(mockedService.getRefresher(anyString(), anyString()))
                .thenReturn(new MountTableRefresher(failedManager, returnsSecondArg().toString()))
                .thenReturn(new MountTableRefresher(successManager, returnsSecondArg().toString()));

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table cache refresher was interrupted.");
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() throws InterruptedException {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(successManager.refresh()).thenReturn(true);
        doAnswer(new AnswersWithDelay(5000, new Returns(true))).when(failedManager).refresh();

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        // smth more
        when(mockedService.getRefresher(anyString(), anyString()))
                .thenReturn(new MountTableRefresher(failedManager, returnsSecondArg().toString()))
                .thenReturn(new MountTableRefresher(successManager, returnsSecondArg().toString()));
        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }
}
