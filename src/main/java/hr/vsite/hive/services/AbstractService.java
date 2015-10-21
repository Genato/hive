package hr.vsite.hive.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import hr.vsite.hive.HiveConfiguration;
import hr.vsite.hive.HiveDestroyEvent;
import hr.vsite.hive.HiveEventBus;
import hr.vsite.hive.HiveInitEvent;
import hr.vsite.hive.HiveStartEvent;
import hr.vsite.hive.HiveStopEvent;

/**
 * Default Service implementation. Extend and provide concrete implementations.
 * Listest on event bus and responda on application's stae events.
 * 
 * @see {@link Service}, {@link HiveInitEvent}, {@link HiveStartEvent}, {@link HiveStopEvent}, {@link HiveDestroyEvent}
 */ 
public abstract class AbstractService implements Service {

	AbstractService(HiveConfiguration conf, HiveEventBus eventBus, String key) {
		this.key = key;
		this.enabled = conf.getBoolean("hive.services." + key + ".Enabled");
		eventBus.register(this);
	}
	
	@Override
	public boolean isEnabled() { return enabled; }

	@Override
	public Service.State getState() { return state; }

	@Subscribe
	public void onHiveInit(HiveInitEvent event) {
		if (!enabled)
			return;
		try {
			init();
		} catch (Exception e) {
			log.error("Error initializing {}", key, e);
		}
	}

	@Subscribe
	public void onHiveStart(HiveStartEvent event) {
		if (!enabled)
			return;
		try {
			start();
		} catch (Exception e) {
			log.error("Error starting {}", key, e);
		}
	}

	@Subscribe
	public void onHiveStop(HiveStopEvent event) {
		if (!enabled || state != Service.State.Running)
			return;
		stop();
	}

	@Subscribe
	public void onHiveDestroy(HiveDestroyEvent event) {
		if (!enabled || state != Service.State.Stopped)
			return;
		destroy();
	}

	@Override
	public void init() throws Exception {
		synchronized (state) {
			if (state != Service.State.Uninitialized)
				throw new IllegalStateException("Cannot initialize " + key + ": service is in state " + state);
			doInit();
			state = Service.State.Stopped;
		}
	}

	@Override
	public void start() throws Exception {
		synchronized (state) {
			if (state != Service.State.Stopped)
				throw new IllegalStateException("Cannot start " + key + ": service is in state " + state);
			doStart();
			state = Service.State.Running;
		}
	}

	@Override
	public void stop() {
		synchronized (state) {
			if (state != Service.State.Running)
				throw new IllegalStateException("Cannot stop " + key + ": service is in state " + state);
			doStop();
			state = Service.State.Stopped;
		}
	}

	@Override
	public void destroy() {
		synchronized (state) {
			if (state != Service.State.Stopped)
				throw new IllegalStateException("Cannot destroy " + key + ": service is in state " + state);
			doDestroy();
			state = Service.State.Destroyed;
		}
	}

	protected abstract void doInit() throws Exception;
	protected abstract void doStart() throws Exception;
	protected abstract void doStop();
	protected abstract void doDestroy();

	private static final Logger log = LoggerFactory.getLogger(AbstractService.class);

	private final String key;
	private final boolean enabled;

	private Service.State state = Service.State.Uninitialized;

}