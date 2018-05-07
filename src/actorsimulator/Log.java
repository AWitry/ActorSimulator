/*
 * Copyright 2018 IronFox.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package actorsimulator;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Centralized message logging
 */
public class Log
{
	public enum Significance
	{
		MinorNetworkEvent,
		MajorNetworkEvent,
		ActorMessage,
		Error
	}
	
	private static final SimpleDateFormat form = new SimpleDateFormat("HH:mm:ss.SSS");
	private static final long STARTED = Calendar.getInstance().getTime().getTime();
	
	static
	{
		form.setTimeZone(TimeZone.getTimeZone("GMT"));	
	}
	
	private static String timestamp()
	{
		long msDelta = Calendar.getInstance().getTime().getTime() - STARTED;
		Date dt = Date.from(Instant.ofEpochMilli(msDelta));
		return form.format(dt);
	}
	
	public static Significance minSignificance = Significance.MajorNetworkEvent;
	
	public static void println(Significance v, Object obj)
	{
		if (obj == null || v.compareTo(minSignificance) < 0)
			return;
		if (v == Significance.Error)
			System.err.println(timestamp()+": "+obj);
		else
			System.out.println(timestamp()+": "+obj);
	}
}
