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
import java.util.Calendar;

/**
 * Centralized message logging
 */
public class Log
{
	public enum Verbosity
	{
		MinorNetworkEvent,
		MajorNetworkEvent,
		ActorMessage,
		Error
	}
	
	private static SimpleDateFormat form = new SimpleDateFormat("HH:mm:ss");

	private static String timestamp()
	{
		return form.format(Calendar.getInstance().getTime());
	}
	
	public static Verbosity minVerbosity = Verbosity.MajorNetworkEvent;
	
	public static void println(Verbosity v, Object obj)
	{
		if (obj == null || v.compareTo(minVerbosity) < 0)
			return;
		System.out.println(timestamp()+": "+obj);
	}
}
